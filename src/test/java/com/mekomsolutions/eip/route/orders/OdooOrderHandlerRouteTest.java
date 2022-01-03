package com.mekomsolutions.eip.route.orders;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_1;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_2;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ORDER_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PROCESS_ORDER;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.mysql.watcher.Event;

import com.mekomsolutions.eip.route.orders.BaseOrderOdooRouteTest;

import ch.qos.logback.classic.Level;

public class OdooOrderHandlerRouteTest extends BaseOrderOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-order-handler";
	
	@EndpointInject("mock:odoo-get-external-id-map")
	private MockEndpoint mockExtIdMapEndpoint;
	
	@EndpointInject("mock:odoo-process-order")
	private MockEndpoint mockProcessOrderEndpoint;
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockExtIdMapEndpoint.reset();
		mockProcessOrderEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_FETCH_RESOURCE).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
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
		final String conceptUuid = "test-concept-uuid";
		Event event = createEvent("test_order", "1", ORDER_UUID_1, op);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("concept", singletonMap("uuid", conceptUuid));
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", conceptUuid);
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty("is-drug-order"));
		assertTrue(exchange.getProperty("is-new", Boolean.class));
	}
	
	@Test
	public void shouldProcessANewDrugOrder() throws Exception {
		final String op = "c";
		final Integer expectedProductId = 5;
		final String drugUuid = "drug-uuid";
		Event event = createEvent("drug_order", "2", ORDER_UUID_2, op);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("type", "drugorder");
		orderResource.put("concept", singletonMap("uuid", "concept-uuid"));
		orderResource.put("drug", singletonMap("uuid", drugUuid));
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", drugUuid);
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("is-drug-order", Boolean.class));
		assertTrue(exchange.getProperty("is-new", Boolean.class));
	}
	
	@Test
	public void shouldProcessARevisionOrder() throws Exception {
		final String orderUuid = "36170d8e-d201-4d94-ae89-0be0b0b6d8ba";
		final String op = "c";
		final Integer expectedProductId = 5;
		final String drugUuid = "drug-uuid";
		Event event = createEvent("drug_order", "3", orderUuid, op);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("type", "drugorder");
		orderResource.put("concept", singletonMap("uuid", "concept-uuid"));
		orderResource.put("drug", singletonMap("uuid", drugUuid));
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", drugUuid);
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("is-drug-order", Boolean.class));
		assertFalse(exchange.getProperty("is-new", Boolean.class));
	}
	
	@Test
	public void shouldProcessADiscontinuationOrder() throws Exception {
		final String orderUuid = "46170d8e-d201-4d94-ae89-0be0b0b6d8ba";
		final String op = "c";
		final Integer expectedProductId = 5;
		final String conceptUuid = "concept-uuid";
		Event event = createEvent("orders", "4", orderUuid, op);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("concept", singletonMap("uuid", conceptUuid));
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", conceptUuid);
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty("is-drug-order"));
		assertFalse(exchange.getProperty("is-new", Boolean.class));
	}
	
	@Test
	public void shouldProcessAVoidedOrder() throws Exception {
		final String op = "u";
		final Integer expectedProductId = 6;
		final String drugUuid = "drug-uuid";
		Event event = createEvent("drug_order", "2", ORDER_UUID_2, op);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("type", "drugorder");
		orderResource.put("concept", singletonMap("uuid", "concept-uuid"));
		orderResource.put("drug", singletonMap("uuid", drugUuid));
		orderResource.put("voided", true);
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", drugUuid);
		mockProcessOrderEndpoint.expectedPropertyReceived("odooProductId", expectedProductId);
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("is-drug-order", Boolean.class));
		assertNull(exchange.getProperty("is-new"));
	}
	
	@Test
	public void shouldFailIfNoProductIsFoundInOdooMatchingTheExternalId() throws Exception {
		final String op = "c";
		Event event = createEvent("test_order", "1", ORDER_UUID_1, op);
		final String conceptUuid = "test-concept-uuid";
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("concept", singletonMap("uuid", conceptUuid));
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(0);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", conceptUuid);
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		final String expectError = "No product found in odoo mapped to uuid: " + conceptUuid;
		assertEquals(expectError, exchange.getProperty("error", EIPException.class).getMessage());
	}
	
	@Test
	public void shouldFailIfMultipleProductsAreFoundInOdooMatchingTheExternalId() throws Exception {
		final String op = "c";
		Event event = createEvent("test_order", "1", ORDER_UUID_1, op);
		final String conceptUuid = "test-concept-uuid";
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("concept", singletonMap("uuid", conceptUuid));
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockProcessOrderEndpoint.expectedMessageCount(0);
		mockExtIdMapEndpoint.expectedPropertyReceived("modelName", "product.product");
		mockExtIdMapEndpoint.expectedPropertyReceived("externalId", conceptUuid);
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		mockExtIdMapEndpoint.assertIsSatisfied();
		mockProcessOrderEndpoint.assertIsSatisfied();
		final String expectError = "Found 2 products in odoo mapped to uuid: " + conceptUuid;
		assertEquals(expectError, exchange.getProperty("error", EIPException.class).getMessage());
	}
	
	@Test
	public void shouldSkipARenewOrderActionThatIsNotSupported() throws Exception {
		Event event = createEvent("test_order", "1", ORDER_UUID_1, "c");
		final String action = "RENEW";
		Map orderResource = singletonMap("action", action);
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		assertMessageLogged(Level.WARN, "Don't know how to handle Order with action: " + action);
	}
	
	@Test
	public void shouldSkipAnUnknownOrderAction() throws Exception {
		Event event = createEvent("test_order", "1", ORDER_UUID_1, "c");
		final String action = "UNKNOWN_ACTION";
		Map orderResource = singletonMap("action", action);
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		
		producerTemplate.send(URI_ORDER_HANDLER, exchange);
		
		assertMessageLogged(Level.WARN, "Don't know how to handle Order with action: " + action);
	}
	
}
