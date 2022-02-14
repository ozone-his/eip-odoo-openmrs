package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_QTY;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_WRITE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MANAGE_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_NON_VOIDED_OBS_PROCESSOR;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;

public class NonVoidedObsToOrderLineProcessorRouteTest extends BasePrpRouteTest {
	
	private static final String ROUTE_ID = "non-voided-obs-to-order-line-processor";
	
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
	public void shouldAddANewLineQuoteIfNoneExists() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		final int sessionCount = 3;
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("value", sessionCount));
		mockManageLineEndpoint.expectedMessageCount(1);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_QTY, sessionCount);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		mockManageLineEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldUpdateTheExistingLineIfQuantityHasChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		final Integer newSessionCount = 3;
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("value", newSessionCount));
		final Integer oldSessionCount = 2;
		Map orderLine = new HashMap();
		orderLine.put("product_uom_qty", oldSessionCount);
		exchange.setProperty(EX_PROP_ORDER_LINE, orderLine);
		mockManageLineEndpoint.expectedMessageCount(1);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_QTY, newSessionCount);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		assertMessageLogged(Level.INFO, "Item quantity changed from " + oldSessionCount + " to " + newSessionCount);
		mockManageLineEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldNotUpdateTheExistingLineIfQuantityHasNotChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		final Integer sessionCount = 3;
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("value", sessionCount));
		Map orderLine = new HashMap();
		orderLine.put("product_uom_qty", sessionCount);
		exchange.setProperty(EX_PROP_ORDER_LINE, orderLine);
		mockManageLineEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		mockManageLineEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "Item quantity is the same as that on the existing item in odoo, nothing to update");
	}
	
}
