/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odooopenmrs.model.Product;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
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

    public SaleOrder getDraftSaleOrderIfExistsByPartnerId(int partnerId) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.SALE_ORDER_MODEL,
                    List.of(asList("partner_id", "=", partnerId), asList("state", "=", "draft")),
                    Constants.orderDefaultAttributes);
            if (records == null) {
                throw new EIPException(
                        String.format("Got null response while fetching for Sale order with partner_id %s", partnerId));
            } else if (records.length == 1) {
                SaleOrder saleOrder = OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrder.class);
                log.info("Sale order exists with partner_id {} sale order {}", partnerId, saleOrder);
                return saleOrder;
            } else if (records.length == 0) {
                log.info("No Sale order found with partner_id {}", partnerId);
                return null;
            } else { // TODO: Handle case where multiple sale order with same id exists
                throw new EIPException(String.format("Multiple Sale order found with partner_id %s", partnerId));
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error(
                    "Error occurred while fetching sale order with partner_id {} error {}",
                    partnerId,
                    e.getMessage(),
                    e);
            throw new CamelExecutionException("Error occurred while fetching sale order", null, e);
        }
    }

    public SaleOrder getDraftSaleOrderIfExistsByVisitId(String visitId) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.SALE_ORDER_MODEL,
                    List.of(asList("client_order_ref", "=", visitId), asList("state", "=", "draft")),
                    Constants.orderDefaultAttributes);
            if (records == null) {
                throw new EIPException(String.format(
                        "Got null response while fetching for Sale order with client_order_ref %s", visitId));
            } else if (records.length == 1) {
                SaleOrder saleOrder = OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrder.class);
                log.info("Sale order exists with client_order_ref {} sale order {}", visitId, saleOrder);
                return saleOrder;
            } else if (records.length == 0) {
                log.info("No Sale order found with client_order_ref {}", visitId);
                return null;
            } else { // TODO: Handle case where multiple sale order with same id exists
                throw new EIPException(String.format("Multiple Sale order found with client_order_ref %s", visitId));
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error(
                    "Error occurred while fetching sale order with client_order_ref {} error {}",
                    visitId,
                    e.getMessage(),
                    e);
            throw new CamelExecutionException("Error occurred while fetching sale order", null, e);
        }
    }

    public List<Integer> getSaleOrderIdsByPartnerId(String partnerId) {
        try {
            Object[] records = odooClient.search(Constants.SALE_ORDER_MODEL, asList("partner_id", "=", partnerId));
            if (records == null) {
                throw new EIPException(
                        String.format("Got null response while fetching for Sale order with partner id %s", partnerId));
            } else if (records.length > 0) {
                List<Integer> saleOrderIdsList = OdooUtils.convertToListOfInteger(records);
                log.info("Sale order ids {} with partner id {} ", saleOrderIdsList, partnerId);
                return saleOrderIdsList;
            } else {
                log.info("No Sale order found with partner id {}", partnerId);
                return null;
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error(
                    "Error occurred while fetching sale order ids with partner id {} error {}",
                    partnerId,
                    e.getMessage(),
                    e);
            throw new CamelExecutionException("Error occurred while fetching sale order ids", null, e);
        }
    }

    public void sendSaleOrder(ProducerTemplate producerTemplate, String endpointUri, SaleOrder saleOrder) {
        Map<String, Object> saleOrderHeaders = new HashMap<>();
        if (endpointUri.contains("update")) {
            saleOrderHeaders.put(com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ATTRIBUTE_NAME, "id");
            saleOrderHeaders.put(
                    com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ATTRIBUTE_VALUE,
                    List.of(saleOrder.getOrderId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, saleOrder, saleOrderHeaders);
    }

    public void cancelSaleOrderIfPatientDeceased(Patient patient, int partnerId, ProducerTemplate producerTemplate) {
        log.info("Is Patient deceased {}", patient.hasDeceased());
        if (patient.hasDeceased() && false) { // TODO: Fix this all the patients are deceased in demo data
            List<Integer> saleOrderPartnerIds = getSaleOrderIdsByPartnerId(String.valueOf(partnerId));
            Map<String, Object> saleOrderHeaders = new HashMap<>();
            saleOrderHeaders.put(com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ATTRIBUTE_NAME, "id");
            saleOrderHeaders.put(
                    com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ATTRIBUTE_VALUE, saleOrderPartnerIds);
            SaleOrder saleOrder = new SaleOrder();
            saleOrder.setOrderState("cancel");
            producerTemplate.sendBodyAndHeaders("direct:odoo-update-sale-order-route", saleOrder, saleOrderHeaders);
        }
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
        log.info(
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
        log.info(
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
            log.info(
                    "{}: Created sale order {} and sale order line {} and linked to sale order",
                    resource.getClass().getName(),
                    fetchedSaleOrder.getOrderId(),
                    saleOrderLine);
        }
    }

    public void deleteSaleOrderLine(
            int partnerId,
            MedicationRequest medicationRequest,
            String encounterVisitUuid,
            ProducerTemplate producerTemplate) {
        SaleOrder saleOrder = getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
        if (saleOrder != null) {
            Product product = productHandler.getProduct(medicationRequest);
            if (product != null) {
                SaleOrderLine saleOrderLine = saleOrderLineHandler.getSaleOrderLineIfExists(
                        saleOrder.getOrderId(), product.getProductResId());
                saleOrderLineHandler.sendSaleOrderLine(
                        producerTemplate, "direct:odoo-delete-sale-order-line-route", saleOrderLine);

                // Check if sale order has no sale order line, then cancel the sale order
                cancelSaleOrderWhenNoSaleOrderLine(partnerId, encounterVisitUuid, producerTemplate);
            }
        }
    }

    private void cancelSaleOrderWhenNoSaleOrderLine(
            int partnerId, String encounterVisitUuid, ProducerTemplate producerTemplate) {
        SaleOrder saleOrder = getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
        if (saleOrder != null
                && (saleOrder.getOrderLine() == null || saleOrder.getOrderLine().isEmpty())) {
            log.info("MedicationRequest: Count of sale order line {}", saleOrder.getOrderLine());
            saleOrder.setOrderState("cancel");
            saleOrder.setOrderPartnerId((Integer) partnerId);
            sendSaleOrder(producerTemplate, "direct:odoo-update-sale-order-route", saleOrder);
        }
    }
}
