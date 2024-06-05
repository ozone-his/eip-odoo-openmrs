package com.ozonehis.eip.odooopenmrs.routes.partner;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odooopenmrs.model.Partner;
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
public class UpdatePartnerRouteTest extends CamelSpringTestSupport {

    private static final String UPDATE_PARTNER_ROUTE = "direct:odoo-update-partner-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new UpdatePartnerRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("odoo-update-partner-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://write/res.partner").replace().to("mock:update-partner");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(UPDATE_PARTNER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldUpdatePartner() throws Exception {
        Partner partner = new Partner();
        partner.setPartnerId(12);
        partner.setPartnerRef("12345");
        partner.setPartnerName("John Doe");

        Map<String, Object> updateHeaders = new HashMap<>();
        updateHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "u");
        updateHeaders.put(
                com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, partner.getPartnerId());

        // Expectations
        MockEndpoint mockUpdatePartnerEndpoint = getMockEndpoint("mock:update-partner");
        mockUpdatePartnerEndpoint.expectedMessageCount(1);
        mockUpdatePartnerEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "u");
        mockUpdatePartnerEndpoint.expectedHeaderReceived(
                com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, 12);
        mockUpdatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(UPDATE_PARTNER_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(updateHeaders);
            exchange.getMessage().setBody(partner);
        });

        // Verify
        mockUpdatePartnerEndpoint.assertIsSatisfied();
    }
}
