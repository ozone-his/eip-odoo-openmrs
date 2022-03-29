package com.mekomsolutions.eip.route.orders;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_PATIENT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_TABLE_RESOURCE_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PROCESS_ORDER;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import ch.qos.logback.classic.Level;

public class OdooProcessOrderRouteTest extends BaseOrderOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-process-order";
	
	public static final String EX_PROP_IS_NEW = "is-new";
	
	public static final String EX_PROP_IS_DRUG_ORDER = "is-drug-order";
	
	public static final String EX_PROP_MODEL_NAME = "modelName";
	
	public static final String EX_PROP_EXT_ID = "externalId";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_ORDER_LINE = "order-line";
	
	public static final String EX_PROP_ORDER_LINE_COUNT = "order-line-count";
	
	public static final String EX_PROP_QUOTE_ID = "quotation-id";
	
	public static final String EX_PROP_UNITS_ID = "unitsId";
	
	public static final String EX_PROP_CREATE_CUSTOMER = "createCustomerIfNotExist";
	
	public static final String EX_PROP_DESC = "description";
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject("mock:odoo-get-draft-quotations")
	private MockEndpoint mockGetDraftQuotesEndpoint;
	
	@EndpointInject("mock:odoo-manage-quotation")
	private MockEndpoint mockManageQuoteEndpoint;
	
	@EndpointInject("mock:odoo-get-order-line")
	private MockEndpoint mockGetOrderLineEndpoint;
	
	@EndpointInject("mock:odoo-get-external-id-map")
	private MockEndpoint mockGetExtIdMapEndpoint;
	
	@EndpointInject("mock:odoo-process-new-order")
	private MockEndpoint mockProcessNewOrderEndpoint;
	
	@EndpointInject("mock:odoo-process-revision-order")
	private MockEndpoint mockProcessRevOrderEndpoint;
	
	@EndpointInject("mock:odoo-process-dc-or-voided-order")
	private MockEndpoint mockProcessDcOrVoidedOrderEndpoint;
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		mockGetDraftQuotesEndpoint.reset();
		mockManageQuoteEndpoint.reset();
		mockGetOrderLineEndpoint.reset();
		mockGetExtIdMapEndpoint.reset();
		mockProcessNewOrderEndpoint.reset();
		mockProcessRevOrderEndpoint.reset();
		mockProcessDcOrVoidedOrderEndpoint.reset();
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_FETCH_RESOURCE).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-patient-handler").skipSendToOriginalEndpoint()
				        .to(mockPatientHandlerEndpoint);
				interceptSendToEndpoint("direct:odoo-get-draft-quotations").skipSendToOriginalEndpoint()
				        .to(mockGetDraftQuotesEndpoint);
				interceptSendToEndpoint("direct:odoo-manage-quotation").skipSendToOriginalEndpoint()
				        .to(mockManageQuoteEndpoint);
				interceptSendToEndpoint("direct:odoo-get-order-line").skipSendToOriginalEndpoint()
				        .to(mockGetOrderLineEndpoint);
				interceptSendToEndpoint("direct:odoo-get-external-id-map").skipSendToOriginalEndpoint()
				        .to(mockGetExtIdMapEndpoint);
				interceptSendToEndpoint("direct:odoo-process-new-order").skipSendToOriginalEndpoint()
				        .to(mockProcessNewOrderEndpoint);
				interceptSendToEndpoint("direct:odoo-process-revision-order").skipSendToOriginalEndpoint()
				        .to(mockProcessRevOrderEndpoint);
				interceptSendToEndpoint("direct:odoo-process-dc-or-voided-order").skipSendToOriginalEndpoint()
				        .to(mockProcessDcOrVoidedOrderEndpoint);
			}
			
		});
	}
	
	@After
	public void tearDown() throws Exception {
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfThereIsMultipleDraftQuotationsForThePatient() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		assertEquals("Found 2 existing draft quotation(s) for the same patient created by this system in odoo",
		    getErrorMessage(exchange));
	}
	
	@Test
	public void shouldProcessANewOrderForAPatientWithNoExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
	}
	
	@Test
	public void shouldProcessARevisionOrderForAPatientWithNoExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
	}
	
	@Test
	public void shouldProcessADiscontinueOrderAndThePatientHasNoExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertFalse(exchange.getProperty(EX_PROP_CREATE_CUSTOMER, Boolean.class));
	}
	
	@Test
	public void shouldProcessAVoidedOrderAndThePatientHasNoExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		orderResource.put("voided", true);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertFalse(exchange.getProperty(EX_PROP_CREATE_CUSTOMER, Boolean.class));
	}
	
	@Test
	public void shouldProcessANewOrderForAPatientWithAnExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		Map orderLine = singletonMap("id", 123);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { orderLine }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(orderLine, exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
	}
	
	@Test
	public void shouldProcessARevisionOrderForAPatientWithAnExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		Map orderLine = singletonMap("id", 123);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { orderLine }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(orderLine, exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
	}
	
	@Test
	public void shouldProcessADiscontinueOrderAndThePatientHasAnExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		Map orderLine = singletonMap("id", 123);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { orderLine }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(orderLine, exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertFalse(exchange.getProperty(EX_PROP_CREATE_CUSTOMER, Boolean.class));
	}
	
	@Test
	public void shouldProcessAVoidedOrderAndThePatientHasAnExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		orderResource.put("voided", true);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		Map orderLine = singletonMap("id", 123);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { orderLine }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(orderLine, exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertFalse(exchange.getProperty(EX_PROP_CREATE_CUSTOMER, Boolean.class));
	}
	
	@Test
	public void shouldFailIfMultipleLinesAreFoundForTheSameOrderableOnAnExistingQuotation() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals("Found 2 lines for the same product added to the draft quotation in odoo", getErrorMessage(exchange));
	}
	
	@Test
	public void shouldProcessOrderIfNoLineIsFoundForTheOrderableOnAnExistingQuotationAndOrderLineCountWasGreaterThanZero()
	    throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		Integer[] orderLines = new Integer[] { 1, 2 };
		quote.put("order_line", orderLines);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		mockGetOrderLineEndpoint.expectedMessageCount(1);
		mockGetOrderLineEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(orderLines.length, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertMessageLogged(Level.INFO, "No order line found on the draft quotation in odoo");
	}
	
	@Test
	public void shouldNotProcessAVoidedOrderForAPatientWithNoOdooRecord() throws Exception {
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		orderResource.put("voided", true);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockGetDraftQuotesEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO,
		    "No action to take for a voided or discontinuation order because the patient has no customer record in odoo");
	}
	
	@Test
	public void shouldNotProcessADiscontinueOrderForAPatientWithNoOdooRecord() throws Exception {
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockGetDraftQuotesEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO,
		    "No action to take for a voided or discontinuation order because the patient has no customer record in odoo");
	}
	
	@Test
	public void shouldProcessAnOrderWithNoExistingLineForAnExistingQuotation() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockGetOrderLineEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		final Integer quoteId = 7;
		Map quote = new HashMap();
		quote.put("id", quoteId);
		quote.put("order_line", new Integer[] {});
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { quote }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetOrderLineEndpoint.assertIsSatisfied();
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertMessageLogged(Level.INFO, "No order line found on the draft quotation in odoo");
	}
	
	@Test
	public void shouldProcessQuantityDetailsAndDosingInstructionsForANewDrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Integer qty = 2;
		final String tabs = "Tabs";
		final String qtyUnitsUuid = "some-units-uuid";
		final Map quantityUnitsResource = new HashMap();
		quantityUnitsResource.put("uuid", qtyUnitsUuid);
		quantityUnitsResource.put("display", tabs);
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("frequency", frequencyResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		orderResource.put("quantity", qty);
		orderResource.put("quantityUnits", quantityUnitsResource);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, "uom.uom");
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXT_ID, qtyUnitsUuid);
		final Integer unitsId = 4;
		mockGetExtIdMapEndpoint
		        .whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { singletonMap("res_id", unitsId) }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(unitsId, exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + daily + ", " + duration + " " + week + " ("
		        + qty + " " + tabs + ")";
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldProcessQuantityDetailsAndDosingInstructionsForANewDrugOrderAppedingOrdererToDescription() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		Map ordererResource = new HashMap();
		ordererResource.put("display", "Test - Test User");
		orderResource.put("orderer", ordererResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Integer qty = 2;
		final String tabs = "Tabs";
		final String qtyUnitsUuid = "some-units-uuid";
		final Map quantityUnitsResource = new HashMap();
		quantityUnitsResource.put("uuid", qtyUnitsUuid);
		quantityUnitsResource.put("display", tabs);
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("frequency", frequencyResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		orderResource.put("quantity", qty);
		orderResource.put("quantityUnits", quantityUnitsResource);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, "uom.uom");
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXT_ID, qtyUnitsUuid);
		final Integer unitsId = 4;
		mockGetExtIdMapEndpoint
		        .whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { singletonMap("res_id", unitsId) }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(unitsId, exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + daily + ", " + duration + " " + week + " ("
		        + qty + " " + tabs + "), " + "Orderer: " + ordererResource.get("display");
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldProcessQuantityDetailsAndDosingInstructionsForARevisionDrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("patient", patientResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(1);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Integer qty = 2;
		final String tabs = "Tabs";
		final String qtyUnitsUuid = "some-units-uuid";
		final Map quantityUnitsResource = new HashMap();
		quantityUnitsResource.put("uuid", qtyUnitsUuid);
		quantityUnitsResource.put("display", tabs);
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("frequency", frequencyResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		orderResource.put("quantity", qty);
		orderResource.put("quantityUnits", quantityUnitsResource);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, "uom.uom");
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXT_ID, qtyUnitsUuid);
		final Integer unitsId = 4;
		mockGetExtIdMapEndpoint
		        .whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { singletonMap("res_id", unitsId) }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(unitsId, exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + daily + ", " + duration + " " + week + " ("
		        + qty + " " + tabs + ")";
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldProcessQuantityDetailsAndDosingInstructionsForARevisionDrugOrderAppedingOrdererToDescription() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "REVISE");
		orderResource.put("patient", patientResource);
		Map ordererResource = new HashMap();
		ordererResource.put("display", "Test - Test User");
		orderResource.put("orderer", ordererResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(1);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Integer qty = 2;
		final String tabs = "Tabs";
		final String qtyUnitsUuid = "some-units-uuid";
		final Map quantityUnitsResource = new HashMap();
		quantityUnitsResource.put("uuid", qtyUnitsUuid);
		quantityUnitsResource.put("display", tabs);
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("frequency", frequencyResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		orderResource.put("quantity", qty);
		orderResource.put("quantityUnits", quantityUnitsResource);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, "uom.uom");
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXT_ID, qtyUnitsUuid);
		final Integer unitsId = 4;
		mockGetExtIdMapEndpoint
		        .whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { singletonMap("res_id", unitsId) }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals(unitsId, exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + daily + ", " + duration + " " + week + " ("
		        + qty + " " + tabs + "), " + "Orderer: " + ordererResource.get("display");
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldNotProcessQuantityDetailsIfTheyAreNoSetOnTheDrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		orderResource.put("drug", singletonMap("display", "Aspirin"));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockGetExtIdMapEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertNull(exchange.getProperty(EX_PROP_UNITS_ID));
	}
	
	@Test
	public void shouldFailIfTheQuantityUnitsAreNotMappedToAnyOdooUnits() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		orderResource.put("drug", singletonMap("display", "Aspirin"));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final String qtyUnitsUuid = "some-units-uuid";
		final Map quantityUnits = singletonMap("uuid", qtyUnitsUuid);
		orderResource.put("quantity", 2);
		orderResource.put("quantityUnits", quantityUnits);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, "uom.uom");
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXT_ID, qtyUnitsUuid);
		mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals("No units of measure found in odoo mapped to uuid: " + qtyUnitsUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfTheQuantityUnitsAreMappedToMultipleOdooUnits() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "NEW");
		orderResource.put("patient", patientResource);
		orderResource.put("drug", singletonMap("display", "Aspirin"));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_TABLE_RESOURCE_MAP, singletonMap("patient", "patientRepository"));
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final String qtyUnitsUuid = "some-units-uuid";
		final Map quantityUnits = singletonMap("uuid", qtyUnitsUuid);
		orderResource.put("quantity", 2);
		orderResource.put("quantityUnits", quantityUnits);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_MODEL_NAME, "uom.uom");
		mockGetExtIdMapEndpoint.expectedPropertyReceived(EX_PROP_EXT_ID, qtyUnitsUuid);
		mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] { emptyMap(), emptyMap() }));
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertEquals("Found 2 units of measure in odoo mapped to uuid: " + qtyUnitsUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldSetDosingInstructionsWithNoDurationForDrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("frequency", frequencyResource);
		mockGetExtIdMapEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertNull(exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + daily;
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldSetDosingInstructionsWithNoQuantityForADrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("frequency", frequencyResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertNull(exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + daily + ", " + duration + " " + week;
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldSetDosingInstructionsWithNoFrequencyForADrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		orderResource.put("patient", patientResource);
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final Double dose = 500.0;
		final String mg = "mg";
		final Map doseUnitsResource = singletonMap("display", mg);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("dose", dose);
		orderResource.put("doseUnits", doseUnitsResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertNull(exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + dose.toString() + mg + ", " + duration + " " + week;
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
	@Test
	public void shouldSetDosingInstructionsWithNoDoseForADrugOrder() throws Exception {
		final Integer odooPatientId = 5;
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		Map orderResource = new HashMap();
		orderResource.put("action", "DISCONTINUE");
		final String drugName = "Aspirin";
		orderResource.put("drug", singletonMap("display", drugName));
		orderResource.put("patient", patientResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, orderResource);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		exchange.setProperty(EX_PROP_IS_DRUG_ORDER, true);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_CUSTOMER, true);
		mockPatientHandlerEndpoint.whenAnyExchangeReceived(e -> e.setProperty(EX_PROP_ODOO_PATIENT_ID, odooPatientId));
		
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, odooPatientId);
		mockGetDraftQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		
		mockManageQuoteEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		final Integer quoteId = 9;
		mockManageQuoteEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quoteId));
		
		final String daily = "Daily";
		final Map frequencyResource = singletonMap("display", daily);
		final Integer duration = 1;
		final String week = "Week";
		final Map durationUnitsResource = singletonMap("display", week);
		orderResource.put("frequency", frequencyResource);
		orderResource.put("duration", duration);
		orderResource.put("durationUnits", durationUnitsResource);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockManageQuoteEndpoint.assertIsSatisfied();
		mockProcessNewOrderEndpoint.assertIsSatisfied();
		mockProcessRevOrderEndpoint.assertIsSatisfied();
		mockProcessDcOrVoidedOrderEndpoint.assertIsSatisfied();
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_ORDER_LINE));
		assertEquals(0, exchange.getProperty(EX_PROP_ORDER_LINE_COUNT));
		assertEquals(quoteId, exchange.getProperty(EX_PROP_QUOTE_ID));
		assertNull(exchange.getProperty(EX_PROP_UNITS_ID));
		final String description = drugName + " " + daily + ", " + duration + " " + week;
		assertEquals(description, exchange.getProperty(EX_PROP_DESC));
	}
	
}
