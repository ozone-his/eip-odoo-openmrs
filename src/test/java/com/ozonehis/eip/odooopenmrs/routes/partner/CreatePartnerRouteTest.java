package com.ozonehis.eip.odooopenmrs.routes.partner;


import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odooopenmrs.model.Partner;
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

import java.util.HashMap;
import java.util.Map;

@UseAdviceWith
public class CreatePartnerRouteTest extends CamelSpringTestSupport {

    private static final String CREATE_PARTNER_ROUTE = "direct:odoo-create-partner-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new CreatePartnerRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    void setup() throws Exception {
        adviceWith("odoo-create-partner-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://create/res.partner")
                        .replace()
                        .to("mock:create-partner");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_PARTNER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreatePartner() throws Exception {
        Partner partner = new Partner();
        partner.setPartnerRef("12345");
        partner.setPartnerName("John Doe");

        Map<String,Object> createHeaders = new HashMap<>();
        createHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "c");

        // Expectations
        MockEndpoint mockCreatePartnerEndpoint = getMockEndpoint("mock:create-partner");
        mockCreatePartnerEndpoint.expectedMessageCount(1);
        mockCreatePartnerEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "c");
        mockCreatePartnerEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_PARTNER_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(createHeaders);
            exchange.getMessage().setBody(partner);
        });

        // Verify
        mockCreatePartnerEndpoint.assertIsSatisfied();
    }
}
