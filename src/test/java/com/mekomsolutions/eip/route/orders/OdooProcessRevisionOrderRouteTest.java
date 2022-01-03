package com.mekomsolutions.eip.route.orders;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_WRITE;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.mekomsolutions.eip.route.orders.BaseOrderOdooRouteTest;
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
	public void shouldIgnoreAnEventForADrugOrderIfQuantityAndUnitsHaveNotChanged() throws Exception {
		final Double quantity = 2.0;
		final Integer unitsId = 5;
		Map existingOrderLine = new HashMap();
		existingOrderLine.put("product_uom_qty", quantity);
		existingOrderLine.put("product_uom", unitsId);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ORDER_LINE, existingOrderLine);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		exchange.setProperty(EX_PROP_QTY, quantity);
		exchange.setProperty(EX_PROP_UNITS_ID, unitsId);
		mockManageOrderLineEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PROCESS_REV_ORDER, exchange);
		
		mockManageOrderLineEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "Order quantity and units are the same as those in odoo, nothing to update");
	}
	
	@Test
	public void shouldUpdateTheExistingOrderLineInOdooIfQuantityHasChangedForADrugOrder() throws Exception {
		final Double oldQty = 2.0;
		final Double newQty = 3.0;
		final Integer unitsId = 5;
		Map existingOrderLine = new HashMap();
		existingOrderLine.put("product_uom_qty", oldQty);
		existingOrderLine.put("product_uom", unitsId);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ORDER_LINE, existingOrderLine);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		exchange.setProperty(EX_PROP_QTY, newQty);
		exchange.setProperty(EX_PROP_UNITS_ID, unitsId);
		mockManageOrderLineEndpoint.expectedMessageCount(1);
		mockManageOrderLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		producerTemplate.send(URI_PROCESS_REV_ORDER, exchange);
		
		mockManageOrderLineEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "Orderable quantity changed from " + oldQty + " to " + newQty);
	}
	
	@Test
	public void shouldUpdateTheExistingOrderLineInOdooIfUnitsHaveChangedForADrugOrder() throws Exception {
		final Double quantity = 2.0;
		final Integer oldUnitsId = 5;
		final Integer newUnitsId = 6;
		Map existingOrderLine = new HashMap();
		existingOrderLine.put("product_uom_qty", quantity);
		existingOrderLine.put("product_uom", oldUnitsId);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ORDER_LINE, existingOrderLine);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		exchange.setProperty(EX_PROP_QTY, quantity);
		exchange.setProperty(EX_PROP_UNITS_ID, newUnitsId);
		mockManageOrderLineEndpoint.expectedMessageCount(1);
		mockManageOrderLineEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		producerTemplate.send(URI_PROCESS_REV_ORDER, exchange);
		
		mockManageOrderLineEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "Orderable quantity units changed from " + oldUnitsId + " to " + newUnitsId);
	}
	
}
