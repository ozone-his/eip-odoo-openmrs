package com.ozonehis.eip.route.orders;

import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.ozonehis.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.ozonehis.eip.route.OdooTestConstants.ODOO_OP_WRITE;
import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;

public class OdooProcessRevisionOrderRouteTest extends BaseOrderOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-process-revision-order";
	
	public static final String URI_PROCESS_REV_ORDER = "direct:odoo-process-revision-order";
	
	public static final String EX_PROP_ORDER_LINE = "order-line";
	
	public static final String EX_PROP_IS_DRUG_ORDER = "is-drug-order";
	
	public static final String EX_PROP_QTY = "order-quantity";
	
	public static final String EX_PROP_UNITS_ID = "unitsId";
	
	@EndpointInject("mock:odoo-manage-order-line")
	private MockEndpoint mockManageOrderLineEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockManageOrderLineEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-manage-order-line").skipSendToOriginalEndpoint()
				        .to(mockManageOrderLineEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldAddAnOrderLineInOdooIfNoneExists() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		mockManageOrderLineEndpoint.expectedMessageCount(1);
		mockManageOrderLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		
		producerTemplate.send(URI_PROCESS_REV_ORDER, exchange);
		
		mockManageOrderLineEndpoint.assertIsSatisfied();
		assertEquals(ODOO_OP_CREATE, exchange.getProperty(EX_PROP_ODOO_OP));
		assertMessageLogged(Level.INFO, "Adding new order line to the quotation");
	}
	
	@Test
	public void shouldIgnoreANonDrugOrderWhenThereIsAnExistingLineInOdoo() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ORDER_LINE, Collections.emptyMap());
		mockManageOrderLineEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PROCESS_REV_ORDER, exchange);
		
		mockManageOrderLineEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "There is nothing to update in odoo about a non drug order");
	}
	
	@Test
	public void shouldUpdateTheExistingOrderLineInOdooForADrugOrder() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ORDER_LINE, Collections.emptyMap());
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockManageOrderLineEndpoint.expectedMessageCount(1);
		mockManageOrderLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		producerTemplate.send(URI_PROCESS_REV_ORDER, exchange);
		
		mockManageOrderLineEndpoint.assertIsSatisfied();
	}
	
}
