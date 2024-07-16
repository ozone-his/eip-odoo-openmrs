/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes.saleorder;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.fhir.Constants;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
public class DeleteSaleOrderRouteTest extends CamelSpringTestSupport {

    private static final String DELETE_SALE_ORDER_ROUTE = "direct:odoo-delete-sale-order-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new DeleteSaleOrderRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("odoo-delete-sale-order-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://unlink/sale.order").replace().to("mock:delete-sale-order");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(DELETE_SALE_ORDER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldDeleteSaleOrder() throws Exception {
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(12);
        saleOrder.setOrderClientOrderRef("12345");
        saleOrder.setOrderState("draft");

        Map<String, Object> deleteHeaders = new HashMap<>();
        deleteHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "d");
        deleteHeaders.put(
                com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, saleOrder.getOrderId());

        // Expectations
        MockEndpoint mockDeleteSaleOrderEndpoint = getMockEndpoint("mock:delete-sale-order");
        mockDeleteSaleOrderEndpoint.expectedMessageCount(1);
        mockDeleteSaleOrderEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "d");
        mockDeleteSaleOrderEndpoint.expectedHeaderReceived(
                com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, 12);
        mockDeleteSaleOrderEndpoint.setResultWaitTime(100);

        // Act
        template.send(DELETE_SALE_ORDER_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(deleteHeaders);
            exchange.getMessage().setBody(saleOrder);
        });

        // Verify
        mockDeleteSaleOrderEndpoint.assertIsSatisfied();
    }
}
