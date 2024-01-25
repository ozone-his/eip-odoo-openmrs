package com.ozonehis.eip.odooopenmrs.route;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class OdooAuthenticationRouteTest extends BaseOdooRouteTest {

    @EndpointInject("mock:http")
    private MockEndpoint mockHttpEndpoint;

    @Test
    public void shouldAuthenticateWithOdoo() throws Exception {
        advise("odoo-event-listener", new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint(OdooTestConstants.ODOO_BASE_URL)
                        .skipSendToOriginalEndpoint()
                        .to(mockHttpEndpoint);
            }
        });
        fail("Not yet implemented");
        mockHttpEndpoint.expectedMessageCount(1);
        mockHttpEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldFailIfTheAuthenticateCredentialsAreWrong() throws Exception {
        fail("Not yet implemented");
    }
}
