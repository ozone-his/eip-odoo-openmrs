package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_QTY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_UNITS_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_WRITE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MANAGE_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_NON_VOIDED_OBS_PROCESSOR;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = NonVoidedObsToOrderLineProcessorRouteTest.PROP_UNITS + "="
        + NonVoidedObsToOrderLineProcessorRouteTest.UNITS)
public class NonVoidedObsToOrderLineProcessorRouteTest extends BasePrpRouteTest {
	
	private static final String ROUTE_ID = "non-voided-obs-to-order-line-processor";
	
	protected static final String PROP_UNITS = "odoo.physio.session.units.name";
	
	protected static final String UNITS = "Units";
	
	@EndpointInject("mock:get-units-id-by-name-from-odoo")
	private MockEndpoint mockGetUnitsByNameEndpoint;
	
	@EndpointInject("mock:odoo-manage-order-line")
	private MockEndpoint mockManageLineEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetUnitsByNameEndpoint.reset();
		mockManageLineEndpoint.reset();
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:get-units-id-by-name-from-odoo").skipSendToOriginalEndpoint()
				        .to(mockGetUnitsByNameEndpoint);
				interceptSendToEndpoint(URI_MANAGE_ORDER_LINE).skipSendToOriginalEndpoint().to(mockManageLineEndpoint);
			}
			
		});
		
		mockGetUnitsByNameEndpoint.expectedMessageCount(1);
		mockGetUnitsByNameEndpoint.expectedPropertyReceived(EX_PROP_UNITS_NAME, UNITS);
	}
	
	@After
	public void tearDown() throws Exception {
		mockGetUnitsByNameEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfMultipleUnitsOfMeasureAreFoundInOdoo() {
		Exchange exchange = new DefaultExchange(camelContext);
		mockGetUnitsByNameEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { 5, 6 }));
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		assertEquals("Found 2 units in odoo matching name: " + UNITS, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfNoUnitsOfMeasureAreFoundInOdooMatchingTheSpecifiedName() {
		Exchange exchange = new DefaultExchange(camelContext);
		mockGetUnitsByNameEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		assertEquals("No units found in odoo matching name: " + UNITS, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldAddANewLineQuoteIfNoneExists() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		final int sessionCount = 3;
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("value", sessionCount));
		mockGetUnitsByNameEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { 4 }));
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
		final Integer unitsId = 5;
		Map orderLine = new HashMap();
		orderLine.put("product_uom_qty", oldSessionCount);
		orderLine.put("product_uom", new Integer[] { unitsId });
		exchange.setProperty(EX_PROP_ORDER_LINE, orderLine);
		mockGetUnitsByNameEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { unitsId }));
		mockManageLineEndpoint.expectedMessageCount(1);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_QTY, newSessionCount);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		assertMessageLogged(Level.INFO, "Item quantity changed from " + oldSessionCount + " to " + newSessionCount);
		mockManageLineEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldUpdateTheExistingLineIfQuantityUnitsHaveChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		final Integer sessionCount = 3;
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("value", sessionCount));
		final Integer oldUnitsId = 5;
		final Integer newUnitsId = 6;
		Map orderLine = new HashMap();
		orderLine.put("product_uom_qty", sessionCount);
		orderLine.put("product_uom", new Integer[] { oldUnitsId });
		exchange.setProperty(EX_PROP_ORDER_LINE, orderLine);
		mockGetUnitsByNameEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { newUnitsId }));
		mockManageLineEndpoint.expectedMessageCount(1);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_QTY, sessionCount);
		mockManageLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		assertMessageLogged(Level.INFO, "Item quantity units changed from " + oldUnitsId + " to " + newUnitsId);
		mockManageLineEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldNotUpdateTheExistingLineIfQuantityAndUnitsHaveNotChanged() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		final Integer sessionCount = 3;
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("value", sessionCount));
		final Integer unitsId = 5;
		Map orderLine = new HashMap();
		orderLine.put("product_uom_qty", sessionCount);
		orderLine.put("product_uom", new Integer[] { unitsId });
		exchange.setProperty(EX_PROP_ORDER_LINE, orderLine);
		mockGetUnitsByNameEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { unitsId }));
		mockManageLineEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_NON_VOIDED_OBS_PROCESSOR, exchange);
		
		mockManageLineEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO,
		    "Item quantity and units are the same as those on the existing item in odoo, nothing to update");
	}
	
}
