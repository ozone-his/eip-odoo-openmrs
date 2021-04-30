package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_TABLE_REPO_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_1;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_2;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ORDER_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PROCESS_ORDER;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.component.entity.DrugOrder;
import org.openmrs.eip.component.entity.Order;
import org.openmrs.eip.component.entity.TestOrder;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.repository.OrderRepository;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;

public class OdooOrderHandlerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-order-handler";
	
	@EndpointInject("mock:odoo-get-external-id-map")
	private MockEndpoint mockExtIdMapEndpoint;
	
	@EndpointInject("mock:odoo-process-order")
	private MockEndpoint mockProcessOrderEndpoint;
	
	@Autowired
	private OrderRepository orderRepo;
	
	@Before
	public void setup() throws Exception {
		mockExtIdMapEndpoint.reset();
		mockProcessOrderEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_EXT_ID).skipSendToOriginalEndpoint().to(mockExtIdMapEndpoint);
				interceptSendToEndpoint(URI_PROCESS_ORDER).skipSendToOriginalEndpoint().to(mockProcessOrderEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldSkipADeleteEvent() throws Exception {
		final String op = "d";
		Event event = createEvent("orders", "1", ORDER_UUID_2, op);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		assertMessageLogged(Level.INFO, "Skipping event with operation: " + op);
	}
	
	@Test
	public void shouldSkipAnUpdateEvent() throws Exception {
		final String op = "u";
		Event event = createEvent("orders", "1", ORDER_UUID_2, op);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		assertMessageLogged(Level.INFO, "Skipping event with operation: " + op);
	}
	
	@Test
	public void shouldProcessANewTestOrder() throws Exception {
		final String op = "c";
		final Integer expectedProductId = 4;
		Event event = createEvent("test_order", "1", ORDER_UUID_1, op);
		TestOrder order = (TestOrder) orderRepo.findByUuid(ORDER_UUID_1);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", order.getConcept().getUuid());
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty("is-drug-order"));
		assertTrue(exchange.getProperty("is-new", Boolean.class));
		assertNull(exchange.getProperty("previousOrder"));
	}
	
	@Test
	public void shouldProcessANewDrugOrder() throws Exception {
		final String op = "c";
		final Integer expectedProductId = 5;
		Event event = createEvent("drug_order", "2", ORDER_UUID_2, op);
		DrugOrder order = (DrugOrder) orderRepo.findByUuid(ORDER_UUID_2);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", order.getDrug().getUuid());
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("is-drug-order", Boolean.class));
		assertTrue(exchange.getProperty("is-new", Boolean.class));
		assertNull(exchange.getProperty("previousOrder"));
	}
	
	@Test
	public void shouldProcessARevisionOrder() throws Exception {
		final String orderId = "36170d8e-d201-4d94-ae89-0be0b0b6d8ba";
		final String op = "c";
		final Integer expectedProductId = 5;
		Event event = createEvent("drug_order", "3", orderId, op);
		DrugOrder order = (DrugOrder) orderRepo.findByUuid(orderId);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", order.getDrug().getUuid());
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("is-drug-order", Boolean.class));
		assertFalse(exchange.getProperty("is-new", Boolean.class));
		assertNull(exchange.getProperty("previousOrder"));
	}
	
	@Test
	public void shouldProcessADiscontinuationOrder() throws Exception {
		final String orderId = "46170d8e-d201-4d94-ae89-0be0b0b6d8ba";
		final String op = "c";
		final Integer expectedProductId = 5;
		Event event = createEvent("orders", "4", orderId, op);
		Order order = orderRepo.findByUuid(orderId);
		assertNotNull(order.getPreviousOrder());
		DrugOrder prevOrder = (DrugOrder) orderRepo.findByUuid(order.getPreviousOrder().getUuid());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("orders", "orderRepository"));
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", prevOrder.getDrug().getUuid());
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty("is-drug-order"));
		assertFalse(exchange.getProperty("is-new", Boolean.class));
		assertEquals(prevOrder, exchange.getProperty("previousOrder"));
	}
	
	@Test
	public void shouldProcessAVoidedOrder() throws Exception {
		final String op = "u";
		final Integer expectedProductId = 6;
		Event event = createEvent("drug_order", "2", ORDER_UUID_2, op);
		DrugOrder order = (DrugOrder) orderRepo.findByUuid(ORDER_UUID_2);
		order.setVoided(true);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", order.getDrug().getUuid());
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("is-drug-order", Boolean.class));
		assertNull(exchange.getProperty("is-new"));
		assertNull(exchange.getProperty("previousOrder"));
	}
	
	@Test
	public void shouldFailIfNoProductIsFoundInOdooMatchingTheExternalId() throws Exception {
		final String op = "c";
		Event event = createEvent("test_order", "1", ORDER_UUID_1, op);
		TestOrder order = (TestOrder) orderRepo.findByUuid(ORDER_UUID_1);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(0);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", order.getConcept().getUuid());
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		final String expectError = "No product found in odoo mapped to uuid: " + order.getConcept().getUuid();
		assertEquals(expectError, exchange.getProperty("error", EIPException.class).getMessage());
	}
	
	@Test
	public void shouldFailIfMultipleProductsAreFoundInOdooMatchingTheExternalId() throws Exception {
		final String op = "c";
		Event event = createEvent("test_order", "1", ORDER_UUID_1, op);
		TestOrder order = (TestOrder) orderRepo.findByUuid(ORDER_UUID_1);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(0);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", order.getConcept().getUuid());
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		final String expectError = "Found 2 products in odoo mapped to uuid: " + order.getConcept().getUuid();
		assertEquals(expectError, exchange.getProperty("error", EIPException.class).getMessage());
	}
	
	@Test
	public void shouldSkipARenewOrderActionThatIsNotSupported() {
		Event event = createEvent("test_order", "1", ORDER_UUID_1, "c");
		final String action = "RENEW";
		Order order = orderRepo.findByUuid(ORDER_UUID_1);
		order.setAction(action);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		assertMessageLogged(Level.WARN, "Don't know how to handle Order with action: " + action);
	}
	
	@Test
	public void shouldSkipAnUnknownOrderAction() {
		Event event = createEvent("test_order", "1", ORDER_UUID_1, "c");
		final String action = "UNKNOWN_ACTION";
		Order order = orderRepo.findByUuid(ORDER_UUID_1);
		order.setAction(action);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, order);
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		assertMessageLogged(Level.WARN, "Don't know how to handle Order with action: " + action);
	}
	
}
