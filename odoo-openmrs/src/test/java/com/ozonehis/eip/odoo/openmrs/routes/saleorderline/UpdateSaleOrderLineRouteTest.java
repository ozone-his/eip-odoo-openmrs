/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes.saleorderline;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
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
public class UpdateSaleOrderLineRouteTest extends CamelSpringTestSupport {

    private static final String UPDATE_SALE_ORDER_ROUTE = "direct:odoo-update-sale-order-line-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new UpdateSaleOrderLineRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("odoo-update-sale-order-line-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://write/sale.order.line").replace().to("mock:update-sale-order-line");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(UPDATE_SALE_ORDER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldUpdateSaleOrderLine() throws Exception {
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        saleOrderLine.setSaleOrderLineId(12);
        saleOrderLine.setSaleOrderLineName("Aspirin 81mg");
        saleOrderLine.setSaleOrderLineOrderId("12345");
        saleOrderLine.setSaleOrderLineProductId("67890");

        Map<String, Object> updateHeaders = new HashMap<>();
        updateHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "u");
        updateHeaders.put(
                com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE,
                saleOrderLine.getSaleOrderLineId());

        // Expectations
        MockEndpoint mockUpdateSaleOrderLineEndpoint = getMockEndpoint("mock:update-sale-order-line");
        mockUpdateSaleOrderLineEndpoint.expectedMessageCount(1);
        mockUpdateSaleOrderLineEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "u");
        mockUpdateSaleOrderLineEndpoint.expectedHeaderReceived(
                com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, 12);
        mockUpdateSaleOrderLineEndpoint.setResultWaitTime(100);

        // Act
        template.send(UPDATE_SALE_ORDER_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(updateHeaders);
            exchange.getMessage().setBody(saleOrderLine);
        });

        // Verify
        mockUpdateSaleOrderLineEndpoint.assertIsSatisfied();
    }
}
