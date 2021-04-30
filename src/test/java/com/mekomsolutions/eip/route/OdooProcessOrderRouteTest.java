package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_PATIENT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_TABLE_REPO_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_2;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PROCESS_ORDER;
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
import org.openmrs.eip.component.entity.Order;
import org.openmrs.eip.component.entity.Patient;
import org.openmrs.eip.component.repository.OrderRepository;
import org.openmrs.eip.component.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;

public class OdooProcessOrderRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-process-order";
	
	public static final String EX_PROP_IS_NEW = "is-new";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_ORDER_LINE = "order-line";
	
	public static final String EX_PROP_ORDER_LINE_COUNT = "order-line-count";
	
	public static final String EX_PROP_QUOTE_ID = "quotation-id";
	
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
	
	@Autowired
	private OrderRepository orderRepo;
	
	@Autowired
	private PatientRepository patientRepo;
	
	@Before
	public void setup() throws Exception {
		mockPatientHandlerEndpoint.reset();
		mockGetDraftQuotesEndpoint.reset();
		mockManageQuoteEndpoint.reset();
		mockGetOrderLineEndpoint.reset();
		mockGetExtIdMapEndpoint.reset();
		mockProcessNewOrderEndpoint.reset();
		mockProcessRevOrderEndpoint.reset();
		mockProcessDcOrVoidedOrderEndpoint.reset();
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockGetDraftQuotesEndpoint.expectedMessageCount(1);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
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
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockGetDraftQuotesEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfThereIsMultipleDraftQuotationsForThePatient() throws Exception {
		final Integer odooPatientId = 5;
		Order order = orderRepo.findByUuid(ORDER_UUID_2);
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
		Order order = orderRepo.findByUuid("16170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("NEW", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
		Order order = orderRepo.findByUuid("36170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("REVISE", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
		Order order = orderRepo.findByUuid("46170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("DISCONTINUE", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
	}
	
	@Test
	public void shouldProcessAVoidedOrderAndThePatientHasNoExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Order order = orderRepo.findByUuid("16170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		order.setVoided(true);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
	}
	
	@Test
	public void shouldProcessANewOrderForAPatientWithAnExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Order order = orderRepo.findByUuid("16170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("NEW", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessNewOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
		Order order = orderRepo.findByUuid("36170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("REVISE", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
		Order order = orderRepo.findByUuid("46170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("DISCONTINUE", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
	public void shouldProcessAVoidedOrderAndThePatientHasAnExistingQuote() throws Exception {
		final Integer odooPatientId = 5;
		Order order = orderRepo.findByUuid("16170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		order.setVoided(true);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockProcessNewOrderEndpoint.expectedMessageCount(0);
		mockProcessRevOrderEndpoint.expectedMessageCount(0);
		mockProcessDcOrVoidedOrderEndpoint.expectedMessageCount(1);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
	}
	
	@Test
	public void shouldFailIfMultipleLinesAreFoundForTheSameOrderableOnAnExistingQuotation() throws Exception {
		final Integer odooPatientId = 5;
		Order order = orderRepo.findByUuid("16170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		assertEquals("NEW", order.getAction());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		exchange.setProperty(EX_PROP_IS_NEW, true);
		mockManageQuoteEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
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
	public void shouldNotProcessAVoidedOrderForAPatientWithNoOdooRecord() throws Exception {
		Order order = orderRepo.findByUuid("16170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		order.setVoided(true);
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockGetDraftQuotesEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO,
		    "No action to take for a voided or discontinuation order because the patient has no customer record in odoo");
	}
	
	@Test
	public void shouldNotProcessADiscontinueOrderForAPatientWithNoOdooRecord() throws Exception {
		Order order = orderRepo.findByUuid("46170d8e-d201-4d94-ae89-0be0b0b6d8ba");
		assertEquals("DISCONTINUE", order.getAction());
		Patient patient = patientRepo.findByUuid(order.getPatient().getUuid());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, order);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		mockGetDraftQuotesEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patient);
		
		producerTemplate.send(URI_PROCESS_ORDER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO,
		    "No action to take for a voided or discontinuation order because the patient has no customer record in odoo");
	}
	
	@Test
	public void shouldProcessAnOrderWithAnExistingLineForAnExistingQuotation() throws Exception {
		
	}
	
	@Test
	public void shouldProcessQuantityDetailsForANewDrugOrder() {
		//Set props unitsId
	}
	
	@Test
	public void shouldProcessQuantityDetailsForARevisionDrugOrder() {
		//Set props unitsId
	}
	
	@Test
	public void shouldNotProcessQuantityDetailsIfTheyAreNoSetOnTheDrugOrder() {
		
	}
	
	@Test
	public void shouldFailIfTheQuantityUnitsAreNotMappedToAnyOdooUnits() {
		
	}
	
	@Test
	public void shouldFailIfTheQuantityUnitsAreMappedToMultipleOdooUnits() {
		
	}
	
	@Test
	public void shouldFailForAnUnKnownOrderAction() {
		
	}
	
}
