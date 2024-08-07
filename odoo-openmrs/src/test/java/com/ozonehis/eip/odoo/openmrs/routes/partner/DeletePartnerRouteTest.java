/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes.partner;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odoo.openmrs.model.Partner;
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
public class DeletePartnerRouteTest extends CamelSpringTestSupport {

    private static final String DELETE_PARTNER_ROUTE = "direct:odoo-delete-partner-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new DeletePartnerRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("odoo-delete-partner-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://unlink/res.partner").replace().to("mock:delete-partner");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(DELETE_PARTNER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldDeletePartner() throws Exception {
        Partner partner = new Partner();
        partner.setPartnerId(12);
        partner.setPartnerRef("12345");
        partner.setPartnerName("John Doe");

        Map<String, Object> deleteHeaders = new HashMap<>();
        deleteHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "d");
        deleteHeaders.put(
                com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, partner.getPartnerId());

        // Expectations
        MockEndpoint mockDeletePartnerEndpoint = getMockEndpoint("mock:delete-partner");
        mockDeletePartnerEndpoint.expectedMessageCount(1);
        mockDeletePartnerEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "d");
        mockDeletePartnerEndpoint.expectedHeaderReceived(
                com.ozonehis.eip.odoo.openmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, 12);
        mockDeletePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(DELETE_PARTNER_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(deleteHeaders);
            exchange.getMessage().setBody(partner);
        });

        // Verify
        mockDeletePartnerEndpoint.assertIsSatisfied();
    }
}
