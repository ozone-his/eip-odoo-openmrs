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
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SalesOrderHandler {

    @Autowired
    private OdooClient odooClient;

    public SaleOrder salesOrderExists(String name) {
        try {
            Object[] records = odooClient.search(Constants.SALE_ORDER_MODEL, asList("client_order_ref", "=", name));
            if ((records != null) && (records.length > 0)) {
                return OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrder.class);
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error("Error while checking if sales order exists with name {} error {}", name, e.getMessage(), e);
        }
        return null;
    }

    public SaleOrder getSalesOrder(String name) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.SALE_ORDER_MODEL,
                    asList(asList("client_order_ref", "=", name)),
                    Constants.orderDefaultAttributes);
            if ((records != null) && (records.length == 1)) {
                log.info("Fetched Sales Order: {}", records[0]);
                SaleOrder fetchedSalesOrder =
                        OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrder.class);
                List<Object> partnerIdList = (List<Object>) fetchedSalesOrder.getOrderPartnerId();
                fetchedSalesOrder.setOrderPartnerId((Integer) partnerIdList.get(0));
                log.info("Fetched Sales Order updated: {}", fetchedSalesOrder);
                return fetchedSalesOrder;
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error("Error while checking if sales order exists with name {} error {}", name, e.getMessage(), e);
        }
        return null;
    }

    public void sendSalesOrder(ProducerTemplate producerTemplate, String endpointUri, SaleOrder saleOrder) {
        var quotationHeaders = new HashMap<String, Object>();
        quotationHeaders.put(Constants.HEADER_ODOO_DOCTYPE, Constants.SALE_ORDER_MODEL);
        quotationHeaders.put(Constants.HEADER_ODOO_RESOURCE, saleOrder);
        quotationHeaders.put(Constants.HEADER_ODOO_ID, saleOrder.getOrderClientOrderRef());

        producerTemplate.sendBodyAndHeaders(endpointUri, saleOrder, quotationHeaders);
    }
}
