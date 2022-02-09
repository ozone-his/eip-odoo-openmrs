package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_CREATE_QUOTE_IF_NOT_EXIST;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_EXTERNAL_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_LINE_CONCEPT;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_LINE_COUNT;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_MODEL_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ORDER_LINE_COUNT;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_PRODUCT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_QUOTE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_RES_PRODUCT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_CONCEPT_LINE_PROCESSOR;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_QUOTES;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MANAGE_QUOTE;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

public class ConceptToOrderLineProcessorRouteTest extends BaseOdooRouteTest {
	
	protected static final String ROUTE_ID = "concept-to-order-line-processor";
	
	protected static final String CONCEPT_UUID = "concept-uuid";
	
	@EndpointInject("mock:odoo-get-external-id-map")
	private MockEndpoint mockExtIdMapEndpoint;
	
	@EndpointInject("mock:odoo-get-draft-quotations")
	private MockEndpoint mockGetDraftQuotesEndpoint;
	
	@EndpointInject("mock:odoo-manage-quotation")
	private MockEndpoint mockManageQuoteEndpoint;
	
	@EndpointInject("mock:odoo-get-order-line")
	private MockEndpoint mockGetOrderLineEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockExtIdMapEndpoint.reset();
		mockGetDraftQuotesEndpoint.reset();
		mockGetOrderLineEndpoint.reset();
		mockManageQuoteEndpoint.reset();
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_EXT_ID).skipSendToOriginalEndpoint().to(mockExtIdMapEndpoint);
				interceptSendToEndpoint(URI_GET_QUOTES).skipSendToOriginalEndpoint().to(mockGetDraftQuotesEndpoint);
				interceptSendToEndpoint(URI_MANAGE_QUOTE).skipSendToOriginalEndpoint().to(mockManageQuoteEndpoint);
				interceptSendToEndpoint(URI_GET_LINE).skipSendToOriginalEndpoint().to(mockGetOrderLineEndpoint);
			}
			
		});
		
		mockExtIdMapEndpoint.expectedMessageCount(1);
		mockExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, ODOO_RES_PRODUCT);
		mockExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXTERNAL_ID, CONCEPT_UUID);
	}
	
	@After
	public void tearDown() throws Exception {
		mockExtIdMapEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfThereIsNoMatchingProductFoundInOdoo() {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		assertEquals("No product found in odoo mapped to uuid: " + CONCEPT_UUID, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfMultipleProductsAreFoundInOdooMappedToTheConcept() {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		assertEquals("Found 2 products in odoo mapped to uuid: " + CONCEPT_UUID, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfThereAreMultipleQuotesForThePatient() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		final Integer expectedProductId = 6;
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_PRODUCT_ID, expectedProductId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
		assertEquals("Found 2 existing draft quotation(s) for the same patient in odoo", getErrorMessage(exchange));
	}
	
	@Test
	public void shouldCreateANewQuoteIfNoneExistsAndCreateQuoteIfNotExistIsSetToTrue() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		exchange.setProperty(EX_PROP_CREATE_QUOTE_IF_NOT_EXIST, true);
		final Integer expectedProductId = 6;
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_PRODUCT_ID, expectedProductId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
		mockManageQuoteEndpoint.assertIsSatisfied();
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(0, exchange.getProperty(EX_PROP_LINE_COUNT));
	}
	
	@Test
	public void shouldNotCreateANewQuoteIfNoneExistsAndCreateQuoteIfNotExistIsNotSetToTrue() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		final Integer expectedProductId = 6;
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_PRODUCT_ID, expectedProductId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		mockManageQuoteEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
		mockManageQuoteEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFetchTheExistingLineOnTheExistingQuoteForThePatient() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		final Integer expectedProductId = 6;
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_PRODUCT_ID, expectedProductId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		mockManageQuoteEndpoint.expectedMessageCount(0);
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		Map orderLine = singletonMap("id", 123);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { orderLine }));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(orderLine, exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
	}
	
	@Test
	public void shouldFetchTheExistingLineOnTheExistingQuoteForThePatientAndSetNoOrderLineOnTheExchangeIfNoneExists()
	    throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		final Integer expectedProductId = 6;
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_PRODUCT_ID, expectedProductId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		mockManageQuoteEndpoint.expectedMessageCount(0);
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
	}
	
	@Test
	public void shouldFailIfThereAreMultipleLinesOnTheExistingQuoteForThePatient() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_LINE_CONCEPT, singletonMap("uuid", CONCEPT_UUID));
		final Integer expectedProductId = 6;
		Map[] expectedBody = new Map[] { singletonMap("res_id", expectedProductId) };
		mockExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedBody));
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_PRODUCT_ID, expectedProductId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		mockManageQuoteEndpoint.expectedMessageCount(0);
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_CONCEPT_LINE_PROCESSOR, exchange);
		
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals("Found 2 items for the same product added to the draft quotation in odoo", getErrorMessage(exchange));
	}
	
}
