/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odoo.openmrs.model.Product;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SaleOrderHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private SaleOrderLineHandler saleOrderLineHandler;

    @Autowired
    private SaleOrderMapper saleOrderMapper;

    @Autowired
    private ProductHandler productHandler;

    public SaleOrder getDraftSaleOrderIfExistsByVisitId(String visitId) {
        Object[] records = odooClient.searchAndRead(
                Constants.SALE_ORDER_MODEL,
                List.of(asList("client_order_ref", "=", visitId), asList("state", "=", "draft")),
                Constants.orderDefaultAttributes);
        if (records == null) {
            throw new EIPException(
                    String.format("Got null response while fetching for Sale order with client_order_ref %s", visitId));
        } else if (records.length == 1) {
            SaleOrder saleOrder = OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrder.class);
            log.debug("Sale order exists with client_order_ref {} sale order {}", visitId, saleOrder);
            return saleOrder;
        } else if (records.length == 0) {
            log.warn("No Sale order found with client_order_ref {}", visitId);
            return null;
        } else {
            log.warn("Multiple Sale order exists with client_order_ref {}", visitId);
            throw new EIPException(String.format("Multiple Sale order found with client_order_ref %s", visitId));
        }
    }

    public void sendSaleOrder(ProducerTemplate producerTemplate, String endpointUri, SaleOrder saleOrder) {
        Map<String, Object> saleOrderHeaders = new HashMap<>();
        if (endpointUri.contains("update")) {
            saleOrderHeaders.put(
                    com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE,
                    List.of(saleOrder.getOrderId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, saleOrder, saleOrderHeaders);
    }

    public void updateSaleOrderIfExistsWithSaleOrderLine(
            Resource resource, SaleOrder saleOrder, String encounterVisitUuid, ProducerTemplate producerTemplate) {
        // If sale order exists create sale order line and link it to sale order
        SaleOrderLine saleOrderLine = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);
        if (saleOrderLine == null) {
            log.info(
                    "{}: Skipping create sale order line for encounter Visit {}",
                    resource.getClass().getName(),
                    encounterVisitUuid);
            return;
        }

        producerTemplate.sendBody("direct:odoo-create-sale-order-line-route", saleOrderLine);
        log.debug(
                "{}: Created sale order line {} and linked to sale order {}",
                resource.getClass().getName(),
                saleOrderLine,
                saleOrder);
    }

    public void createSaleOrderWithSaleOrderLine(
            Resource resource,
            Encounter encounter,
            int partnerId,
            String encounterVisitUuid,
            ProducerTemplate producerTemplate) {
        // If the sale order does not exist, create it, then create sale order line and link it to sale order
        SaleOrder newSaleOrder = saleOrderMapper.toOdoo(encounter);
        newSaleOrder.setOrderPartnerId(partnerId);
        newSaleOrder.setOrderState("draft");

        sendSaleOrder(producerTemplate, "direct:odoo-create-sale-order-route", newSaleOrder);
        log.debug(
                "{}: Created sale order with partner_id {}", resource.getClass().getName(), partnerId);

        SaleOrder fetchedSaleOrder = getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
        if (fetchedSaleOrder != null) {
            SaleOrderLine saleOrderLine =
                    saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, fetchedSaleOrder);
            if (saleOrderLine == null) {
                log.info(
                        "{}: Skipping create sale order line and sale order for partner_id {}",
                        resource.getClass().getName(),
                        partnerId);
                return;
            }

            producerTemplate.sendBody("direct:odoo-create-sale-order-line-route", saleOrderLine);
            log.debug(
                    "{}: Created sale order {} and sale order line {} and linked to sale order",
                    resource.getClass().getName(),
                    fetchedSaleOrder.getOrderId(),
                    saleOrderLine);
        }
    }

    public void deleteSaleOrderLine(Resource resource, String encounterVisitUuid, ProducerTemplate producerTemplate) {
        SaleOrder saleOrder = getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
        if (saleOrder != null) {
            Product product = productHandler.getProduct(resource);
            if (product != null) {
                SaleOrderLine saleOrderLine = saleOrderLineHandler.getSaleOrderLineIfExists(
                        saleOrder.getOrderId(), product.getProductResId());
                if (saleOrderLine != null) {
                    saleOrderLineHandler.sendSaleOrderLine(
                            producerTemplate, "direct:odoo-delete-sale-order-line-route", saleOrderLine);
                }
            }
        }
    }

    // Check if sale order has no sale order line, then cancel the sale order
    public void cancelSaleOrderWhenNoSaleOrderLine(
            int partnerId, String encounterVisitUuid, ProducerTemplate producerTemplate) {
        SaleOrder saleOrder = getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
        if (saleOrder != null
                && (saleOrder.getOrderLine() == null || saleOrder.getOrderLine().isEmpty())) {
            log.debug("SaleOrderHandler: Count of sale order line {}", saleOrder.getOrderLine());
            saleOrder.setOrderState("cancel");
            saleOrder.setOrderPartnerId((Integer) partnerId);
            sendSaleOrder(producerTemplate, "direct:odoo-update-sale-order-route", saleOrder);
        }
    }
}
