/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.handlers;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Setter
@Component
public class SalesOrderHandler {

    @Autowired
    private OdooClient odooClient;

    public boolean salesOrderExists(String name) {
        List<List<List<Object>>> searchQuery = Collections.singletonList(
                Collections.singletonList(Arrays.asList("name", "=", name)));
        try {
            Object[] records = (Object[]) odooClient.execute(Constants.SEARCH_METHOD, Constants.SALE_ORDER_MODEL, searchQuery, null);
            if ((records != null) && (records.length > 0)) {
                return true;
            }
        } catch (XmlRpcException e) {
            log.error("Error while checking if sales order exists with name {} error {}", name, e.getMessage(), e);
        }
        return false;
    }

    public SaleOrder getSalesOrder(String name) {
        List<List<List<Object>>> searchQuery = Collections.singletonList(
                Collections.singletonList(Arrays.asList("name", "=", name)));
        try {
            Object[] records = (Object[]) odooClient.execute(Constants.SEARCH_READ_METHOD, Constants.SALE_ORDER_MODEL, searchQuery, null);
            if ((records != null) && (records.length == 1)) {
                return (SaleOrder) records[0];
            }
        } catch (XmlRpcException e) {
            log.error("Error while checking if sales order exists with name {} error {}", name, e.getMessage(), e);
        }
        return null;
    }

    public void sendSalesOrder(ProducerTemplate producerTemplate, String endpointUri, SaleOrder saleOrder) {
        var quotationHeaders = new HashMap<String, Object>();
        quotationHeaders.put(Constants.HEADER_ODOO_DOCTYPE, Constants.SALE_ORDER_MODEL);
        quotationHeaders.put(Constants.HEADER_ODOO_RESOURCE, saleOrder);
        quotationHeaders.put(Constants.HEADER_ODOO_ID, saleOrder.getOrderName());

        producerTemplate.sendBodyAndHeaders(endpointUri, saleOrder, quotationHeaders);
    }
}
