/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.processors;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.handlers.SalesOrderHandler;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncounterProcessor implements Processor {

    @Autowired
    private SalesOrderHandler salesOrderHandler;

    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getMessage();
        Encounter encounter = message.getBody(Encounter.class);
        if (encounter != null && encounter.hasPeriod() && encounter.getPeriod().hasEnd()) {
            SaleOrder saleOrder = salesOrderHandler.getSalesOrder(encounter.getIdPart());
            if (saleOrder != null) {
                saleOrder.setOrderInvoiceStatus("submitted");//TODO: Check
                salesOrderHandler.sendSalesOrder(
                        exchange.getContext().createProducerTemplate(),
                        "direct:odoo-update-sales-order-route",
                        saleOrder);
            } else {
                exchange.setProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER, true);
            }
        } else {
            // skipping the processing of the encounter
            exchange.setProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER, true);
        }
    }
}
