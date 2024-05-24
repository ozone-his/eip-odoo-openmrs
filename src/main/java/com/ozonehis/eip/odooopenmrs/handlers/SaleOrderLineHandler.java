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
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odooopenmrs.model.Product;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import com.ozonehis.eip.odooopenmrs.model.Uom;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SaleOrderLineHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private SalesOrderHandler salesOrderHandler;

    @Autowired
    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    public SaleOrderLine saleOrderLineExists(int saleOrderId, String productId) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.SALE_ORDER_LINE_MODEL,
                    asList(asList("order_id", "=", saleOrderId), asList("product_id", "=", productId)),
                    null);
            if ((records != null) && (records.length > 0)) {
                SaleOrderLine saleOrderLine =
                        OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrderLine.class);
                log.info("SaleOrderLineHandler: saleOrderLineExists saleOrderLine: {}", saleOrderLine);
                return saleOrderLine;
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error(
                    "Error while checking if sales order line exists with name {} error {}",
                    saleOrderId,
                    e.getMessage(),
                    e);
        }
        return null;
    }

    public Integer createSaleOrderLineIfProductExists(Resource resource, SaleOrder saleOrder) {
        Product product = getProduct(getItemName(resource));
        log.info("SaleOrderLineHandler: createSaleOrderLineIfProductExists  product {}", product);
        if (product != null) {
            SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(resource);
            saleOrderLine.setSaleOrderLineOrderId(saleOrder.getOrderId());
            if (resource instanceof MedicationRequest) {
                saleOrderLine.setSaleOrderLineProductUom(
                        (Integer) getUom((String) saleOrderLine.getSaleOrderLineProductUom())
                                .getUomResId());
            } else if (resource instanceof ServiceRequest) {
                saleOrderLine.setSaleOrderLineProductUom(
                        1); // TODO: Hardcoded to 1 so that `Units` is shown for ServiceRequest
            }
            saleOrderLine.setSaleOrderLineProductId(product.getProductResId());
            log.info(
                    "SaleOrderLineHandler: createSaleOrderLineIfProductExists setSaleOrderLineOrderId {}",
                    saleOrderLine);
            try {
                Object[] records = (Object[]) odooClient.execute(
                        Constants.CREATE_METHOD,
                        Constants.SALE_ORDER_LINE_MODEL,
                        List.of(OdooUtils.convertObjectToMap(saleOrderLine)),
                        null);
                if ((records != null) && (records.length > 0)) {
                    log.info(
                            "SaleOrderLineHandler: createSaleOrderLineIfProductExists saleOrderLineId: {}", records[0]);
                    return (Integer) records[0];
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        log.info(
                "SaleOrderLineHandler: createSaleOrderLineIfProductExists unable to create saleOrderLine with product {}",
                product);
        return null;
    }

    private String getItemName(Resource resource) {
        if (resource instanceof ServiceRequest serviceRequest) {
            log.info(
                    "SaleOrderLineHandler: serviceRequest getItemName: {}",
                    serviceRequest.getCode().getCodingFirstRep().getCode());
            return serviceRequest.getCode().getCodingFirstRep().getCode();
        } else if (resource instanceof MedicationRequest medicationRequest) {
            log.info(
                    "SaleOrderLineHandler: medicationRequest getItemName: {}",
                    medicationRequest.getMedicationReference().getReference().split("/")[1]);
            return medicationRequest.getMedicationReference().getReference().split("/")[1];
        } else {
            throw new IllegalArgumentException(
                    "Unsupported resource type: " + resource.getClass().getName());
        }
    }

    // TODO: Fix to fetch UUID of drug
    private String getProductUuid(Resource resource) {
        if (resource instanceof ServiceRequest serviceRequest) {
            return serviceRequest.getCode().getCodingFirstRep().getCode();
        } else if (resource instanceof MedicationRequest medicationRequest) {
            return medicationRequest.getMedicationReference().getReference().split("/")[1];
        } else {
            throw new IllegalArgumentException(
                    "Unsupported resource type: " + resource.getClass().getName());
        }
    }

    public Uom getUom(String name) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.IR_MODEL,
                    asList(asList("model", "=", Constants.UOM_MODEL), asList("name", "=", name)),
                    null);
            if ((records != null) && (records.length > 0)) {
                log.info("Fetched uom {} from Odoo with id {}", records[0], name);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Uom.class);
            } else {
                log.info("No uom found with id {}", name);
                throw new EIPException(String.format("No uom found with id %s", name));
            }
        } catch (MalformedURLException | XmlRpcException e) {
            throw new RuntimeException(
                    String.format("Error occurred while fetching uom from odoo with id %s", name), e);
        }
    }

    public Product getProduct(String externalId) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.IR_MODEL,
                    asList(asList("model", "=", Constants.PRODUCT_MODEL), asList("name", "=", externalId)),
                    null);
            if ((records != null) && (records.length == 1)) {
                log.info("Fetched product {} from Odoo with id {}", records[0], externalId);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Product.class);
            }
        } catch (MalformedURLException | XmlRpcException e) {
            throw new RuntimeException(
                    String.format("Error occurred while fetching product from odoo with id %s", externalId), e);
        }
        log.info("No product found with id {}", externalId);
        return null;
    }
}
