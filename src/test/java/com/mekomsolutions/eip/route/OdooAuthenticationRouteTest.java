package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_BASE_URL;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.eip.BaseCamelTest;

@Ignore
public class OdooAuthenticationRouteTest extends BaseCamelTest {
	
	@EndpointInject("mock:http")
	private MockEndpoint mockHttpEndpoint;
	
	@Test
	public void shouldAuthenticateWithOdoo() throws Exception {
		advise("odoo-event-listener", new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(ODOO_BASE_URL).skipSendToOriginalEndpoint().to(mockHttpEndpoint);
			}
		});
		Assert.fail("Not yet implemented");
		mockHttpEndpoint.expectedMessageCount(1);
		mockHttpEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfTheAuthenticateCredentialsAreWrong() throws Exception {
		Assert.fail("Not yet implemented");
	}
	
}
