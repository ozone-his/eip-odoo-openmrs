package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_UNLINK;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MANAGE_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_VOIDED_OBS_PROCESSOR;

import java.util.Collections;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

public class VoidedObsToOrderLineProcessorRouteTest extends BasePrpRouteTest {
	
	private static final String ROUTE_ID = "voided-obs-to-order-line-processor";
	
	@EndpointInject("mock:odoo-manage-order-line")
	private MockEndpoint mockManageLineEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockManageLineEndpoint.reset();
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_MANAGE_ORDER_LINE).skipSendToOriginalEndpoint().to(mockManageLineEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldRemoveTheExistingItemFromOdoo() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ORDER_LINE, Collections.emptyMap());
		mockManageLineEndpoint.expectedMessageCount(1);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_UNLINK);
		
		producerTemplate.send(URI_VOIDED_OBS_PROCESSOR, exchange);
		
		mockManageLineEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldDoNothingIfThereIsNoItemInOdoo() throws Exception {
		mockManageLineEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_VOIDED_OBS_PROCESSOR, new DefaultExchange(camelContext));
		
		mockManageLineEndpoint.assertIsSatisfied();
		
	}
	
}
