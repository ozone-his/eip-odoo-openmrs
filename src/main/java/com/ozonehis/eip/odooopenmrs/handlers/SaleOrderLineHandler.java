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
import org.apache.camel.CamelExecutionException;
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
    private ProductHandler productHandler;

    @Autowired
    private UomHandler uomHandler;

    @Autowired
    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    public int createSaleOrderLine(SaleOrderLine saleOrderLine) {
        try {
            Object[] records = (Object[]) odooClient.create(Constants.CREATE_METHOD, Constants.SALE_ORDER_LINE_MODEL, List.of(OdooUtils.convertObjectToMap(saleOrderLine)), null);
            if (records == null) {
                throw new EIPException(String.format("Got null response while creating for Sale order line with %s", saleOrderLine));
            } else if (records.length == 1) {
                log.info("Sale order line created with id {} ", records[0]);
                return (Integer) records[0];
            } else {
                throw new EIPException(String.format("Unable to create Sale order line with %s", saleOrderLine));
            }
        } catch (Exception e) {
            log.error("Error occurred while creating sales order line with {} error {}", saleOrderLine, e.getMessage(), e);
            throw new CamelExecutionException("Error occurred while creating sales order line", null, e);
        }
    }

    public Integer createSaleOrderLineIfProductExists(Resource resource, SaleOrder saleOrder) {
        Product product = productHandler.getProduct(resource);
        log.info("SaleOrderLineHandler: Fetched Product {}", product);

        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(resource);
        saleOrderLine.setSaleOrderLineProductId(product.getProductResId());
        saleOrderLine.setSaleOrderLineOrderId(saleOrder.getOrderId());
        if (resource instanceof MedicationRequest) {
            String uomExternalId = (String) saleOrderLine.getSaleOrderLineProductUom();
            Uom uom = uomHandler.getUom(uomExternalId);
            log.info("SaleOrderLineHandler: Fetched Uom {}", uom);
            // Store Uom res_id in productUom to display unit in Odoo
            saleOrderLine.setSaleOrderLineProductUom(uom.getUomResId());
        } else if (resource instanceof ServiceRequest) {
            // Hardcoded to 1 so that `Units` is shown for ServiceRequest
            saleOrderLine.setSaleOrderLineProductUom(1);
        }

        return createSaleOrderLine(saleOrderLine);
    }

//    public SaleOrderLine getSaleOrderLineIfExists(int saleOrderId, String productId) {
//        try {
//            Object[] records = odooClient.searchAndRead(
//                    Constants.SALE_ORDER_LINE_MODEL,
//                    asList(asList("order_id", "=", saleOrderId), asList("product_id", "=", productId)),
//                    null);
//            if (records == null) {
//                throw new EIPException(String.format("Got null response while fetching for Sale order line with sale order id %s product id %s", saleOrderId, productId));
//            } else if (records.length == 1) {
//                SaleOrderLine saleOrderLine =
//                        OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrderLine.class);
//                log.info("Sale order line exists with sale order id {} product id {} sale order line {}", saleOrderId, productId, saleOrderLine);
//                return saleOrderLine;
//            } else if (records.length == 0) {
//                log.info("No Sale order line found with sale order id {} product id {}", saleOrderId, productId);
//                return null;
//            } else { //TODO: Handle case where multiple sale order lines with same sale order id and product id
//                throw new EIPException(String.format("Multiple Sale order line found with sale order id %s product id %s", saleOrderId, productId));
//            }
//        } catch (XmlRpcException | MalformedURLException e) {
//            log.error("Error occurred while fetching sales order line with sale order id {} product id {} error {}", saleOrderId, productId, e.getMessage(), e);
//            throw new CamelExecutionException("Error occurred while fetching sales order line", null, e);
//        }
//    }
}
