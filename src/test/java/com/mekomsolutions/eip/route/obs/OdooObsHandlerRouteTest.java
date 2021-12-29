package com.mekomsolutions.eip.route.obs;

import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_HANDLER;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.BaseOdooRouteTest;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/*.xml,classpath*:camel/obs/*.xml")
@TestPropertySource(properties = "eip.watchedTables=obs")
@TestPropertySource(properties = "odoo.handler.route=" + OdooObsHandlerRouteTest.ROUTE_ID)
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=obs:obs")
@TestPropertySource(properties = "odoo.obs.concept.question.answer.mappings=1:2")
public class OdooObsHandlerRouteTest extends BaseOdooRouteTest {
	
	protected static final String ROUTE_ID = "odoo-obs-handler";
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockPatientHandlerEndpoint.reset();
		mockFetchResourceEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-fetch-resource").skipSendToOriginalEndpoint()
				        .to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-patient-handler").skipSendToOriginalEndpoint()
				        .to(mockPatientHandlerEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldProcessAnInsertedObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
}
