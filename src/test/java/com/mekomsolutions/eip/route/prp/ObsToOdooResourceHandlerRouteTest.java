package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_OBS_TO_ODOO_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_CUSTOMER;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_RES_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_SAVE_CALENDAR_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_TO_CUSTOMER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_TO_RES_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_SAVE_CALENDAR_EVENT;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = APP_PROP_NAME_OBS_TO_ODOO_HANDLER + "=" + ROUTE_ID_OBS_TO_RES_HANDLER)
public class ObsToOdooResourceHandlerRouteTest extends BasePrpRouteTest {
	
	@EndpointInject("mock:" + ROUTE_ID_OBS_TO_CUSTOMER)
	private MockEndpoint mockObsToCustomerEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_SAVE_CALENDAR_EVENT)
	private MockEndpoint mockSaveCalendarEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockObsToCustomerEndpoint.reset();
		mockSaveCalendarEndpoint.reset();
		
		advise(ROUTE_ID_OBS_TO_RES_HANDLER, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_OBS_TO_CUSTOMER).skipSendToOriginalEndpoint().to(mockObsToCustomerEndpoint);
				interceptSendToEndpoint(URI_SAVE_CALENDAR_EVENT).skipSendToOriginalEndpoint().to(mockSaveCalendarEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldCallTheAppropriateRoutes() throws Exception {
		mockObsToCustomerEndpoint.assertIsSatisfied();
		mockSaveCalendarEndpoint.assertIsSatisfied();
		
		producerTemplate.send(URI_OBS_TO_RES_HANDLER, new DefaultExchange(camelContext));
		
		mockObsToCustomerEndpoint.assertIsSatisfied();
		mockSaveCalendarEndpoint.assertIsSatisfied();
	}
	
}
