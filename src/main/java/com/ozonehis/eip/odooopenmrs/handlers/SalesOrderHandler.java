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
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SalesOrderHandler {

    @Autowired
    private OdooClient odooClient;

    public SaleOrder getSalesOrderIfExists(String id) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.SALE_ORDER_MODEL,
                    List.of(asList("client_order_ref", "=", id)),
                    Constants.orderDefaultAttributes);
            if (records == null) {
                throw new EIPException(String.format("Got null response while fetching for Sale order with id %s", id));
            } else if (records.length == 1) {
                SaleOrder saleOrder = OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrder.class);
                log.info("Sale order exists with id {} sale order {}", id, saleOrder);
                return saleOrder;
            } else if (records.length == 0) {
                log.info("No Sale order found with id {}", id);
                return null;
            } else { // TODO: Handle case where multiple sale order with same id exists
                throw new EIPException(String.format("Multiple Sale order found with id %s", id));
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error("Error occurred while fetching sales order with id {} error {}", id, e.getMessage(), e);
            throw new CamelExecutionException("Error occurred while fetching sales order", null, e);
        }
    }

    public void sendSalesOrder(ProducerTemplate producerTemplate, String endpointUri, SaleOrder saleOrder) {
        Map<String, Object> saleOrderHeaders = new HashMap<>();
        saleOrderHeaders.put(Constants.HEADER_ODOO_ID, saleOrder.getOrderClientOrderRef());
        saleOrderHeaders.put(com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ATTRIBUTE_NAME, "client_order_ref");
        saleOrderHeaders.put(
                com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ATTRIBUTE_VALUE, saleOrder.getOrderClientOrderRef());

        producerTemplate.sendBodyAndHeaders(endpointUri, saleOrder, saleOrderHeaders);
    }
}
