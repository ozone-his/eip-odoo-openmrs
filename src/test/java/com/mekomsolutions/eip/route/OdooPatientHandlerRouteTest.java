package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_PATIENT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_WRITE;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_HANDLER;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

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
import org.openmrs.eip.component.entity.PersonAddress;
import org.openmrs.eip.component.entity.PersonName;
import org.openmrs.eip.component.repository.PatientRepository;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;

public class OdooPatientHandlerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-patient-handler";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_CREATE_PATIENT = "createPatient";
	
	@EndpointInject("mock:odoo-get-customer")
	private MockEndpoint mockGetCustomerEndpoint;
	
	@EndpointInject("mock:odoo-manage-customer")
	private MockEndpoint mockManageCustomerEndpoint;
	
	@EndpointInject("mock:odoo-get-quotations")
	private MockEndpoint mockGetQuotesEndpoint;
	
	@EndpointInject("mock:odoo-cancel-quotations")
	private MockEndpoint mockCancelQuotesEndpoint;
	
	@EndpointInject("mock:odoo-process-person-address")
	private MockEndpoint mockProcessAddressEndpoint;
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Autowired
	private PatientRepository patientRepo;
	
	@Before
	public void setup() throws Exception {
		mockGetCustomerEndpoint.reset();
		mockManageCustomerEndpoint.reset();
		mockProcessAddressEndpoint.reset();
		mockFetchResourceEndpoint.reset();
		mockGetQuotesEndpoint.reset();
		mockCancelQuotesEndpoint.reset();
		mockGetCustomerEndpoint.expectedMessageCount(1);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-get-customer").skipSendToOriginalEndpoint().to(mockGetCustomerEndpoint);
				interceptSendToEndpoint("direct:odoo-manage-customer").skipSendToOriginalEndpoint()
				        .to(mockManageCustomerEndpoint);
				interceptSendToEndpoint(URI_FETCH_RESOURCE).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-process-person-address").skipSendToOriginalEndpoint()
				        .to(mockProcessAddressEndpoint);
				interceptSendToEndpoint("direct:odoo-get-quotations").skipSendToOriginalEndpoint().to(mockGetQuotesEndpoint);
				interceptSendToEndpoint("direct:odoo-cancel-quotations").skipSendToOriginalEndpoint()
				        .to(mockCancelQuotesEndpoint);
			}
			
		});
	}
	
	@After
	public void tearDown() throws Exception {
		mockGetCustomerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfThePatientHasMultipleCustomerRecordsInOdoo() {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_PATIENT, patientRepo.findByUuid(PATIENT_UUID));
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { 1, 2 }));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertEquals("Found 2 existing customers in odoo with ref: " + PATIENT_UUID, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldSkipPatientsWithNoCustomerRecordAndTheEventIsForAPatient() {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, patientRepo.findByUuid(PATIENT_UUID));
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.DEBUG, "Patient has no customer record in Odoo, there is nothing to be processed");
	}
	
	@Test
	public void shouldSkipPatientsWithNoCustomerRecordAndTheEventIsForAPersonName() {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, new PersonName());
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.DEBUG, "Patient has no customer record in Odoo, there is nothing to be processed");
	}
	
	@Test
	public void shouldSkipPatientsWithNoCustomerRecordAndTheEventIsForAPersonAddress() {
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, new PersonAddress());
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.DEBUG, "Patient has no customer record in Odoo, there is nothing to be processed");
	}
	
	@Test
	public void shouldAddPatientToOdooIfTheyDoNotExistAddCreatePatientPropIsSetToTrue() throws Exception {
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, new Order());
		exchange.setProperty(EX_PROP_CREATE_PATIENT, true);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		Map personResource = new HashMap();
		final String name = "Test User";
		personResource.put("display", name);
		Map addressResource = new HashMap();
		addressResource.put("address1", "25 Ocean drive");
		personResource.put("preferredAddress", addressResource);
		Map patientResource = singletonMap("person", personResource);
		final String patientJson = new ObjectMapper().writeValueAsString(patientResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		
		mockProcessAddressEndpoint.expectedMessageCount(1);
		mockProcessAddressEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		final int countryId = 4;
		final int stateId = 5;
		final int patientId = 12;
		mockProcessAddressEndpoint.whenAnyExchangeReceived(e -> {
			e.setProperty("odooCountryId", countryId);
			e.setProperty("odooStateId", stateId);
		});
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patient);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdDisplay", "OpenMRS Id: 12345");
		mockManageCustomerEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("odooCountryId", countryId);
		mockManageCustomerEndpoint.expectedPropertyReceived("odooStateId", stateId);
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody(patientId);
		});
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldAddPatientWithoutAddressToOdooIfTheyDoNotExistAndCreatePatientPropIsSetToTrue() throws Exception {
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, new Order());
		exchange.setProperty(EX_PROP_CREATE_PATIENT, true);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		mockProcessAddressEndpoint.expectedMessageCount(0);
		
		Map personResource = new HashMap();
		final String name = "Test User";
		personResource.put("display", name);
		Map patientResource = singletonMap("person", personResource);
		final String patientJson = new ObjectMapper().writeValueAsString(patientResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patient);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdDisplay", "OpenMRS Id: 12345");
		final int patientId = 12;
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientId));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
		assertMessageLogged(Level.DEBUG, "Patient has no address");
	}
	
	@Test
	public void shouldUpdatePatientInOdooIfTheyAlreadyExist() throws Exception {
		final int patientId = 16;
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, new Order());
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { patientId }));
		
		Map personResource = new HashMap();
		final String name = "Another Test User";
		personResource.put("display", name);
		Map addressResource = new HashMap();
		addressResource.put("address1", "26 Oceanic drive");
		personResource.put("preferredAddress", addressResource);
		Map patientResource = singletonMap("person", personResource);
		final String patientJson = new ObjectMapper().writeValueAsString(patientResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		
		mockProcessAddressEndpoint.expectedMessageCount(1);
		mockProcessAddressEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patient);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdDisplay", "OpenMRS Id: 12345");
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientId));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldNotAddAVoidedPatientToOdooIfTheyDoNotExist() throws Exception {
		Event event = createEvent("orders", "1", "some-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		patient.setPatientVoided(true);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, new Order());
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.INFO, "No action to take for voided patient since they have no customer record in odoo");
	}
	
	@Test
	public void shouldInactiveCustomerAndCancelTheirQuotationsForAVoidedPatientThatExistsInOdoo() throws Exception {
		final Integer patientId = 18;
		Event event = createEvent("patient", "1", PATIENT_UUID, "u");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		patient.setPatientVoided(true);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, patient);
		exchange.setProperty(PROP_EVENT, event);
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockProcessAddressEndpoint.expectedMessageCount(0);
		
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { patientId }));
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		mockGetQuotesEndpoint.expectedMessageCount(1);
		mockGetQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, patientId);
		final Integer[] quotationIds = new Integer[] { 1, 2 };
		mockGetQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quotationIds));
		
		mockCancelQuotesEndpoint.expectedMessageCount(1);
		mockCancelQuotesEndpoint.expectedPropertyReceived("quotationIds", quotationIds);
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetQuotesEndpoint.assertIsSatisfied();
		mockCancelQuotesEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldInactiveCustomerAndCancelTheirQuotationsForADeletedPatientThatExistsInOdoo() throws Exception {
		final Integer patientId = 18;
		Event event = createEvent("patient", "1", PATIENT_UUID, "d");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		patient.setPatientVoided(true);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, patient);
		exchange.setProperty(PROP_EVENT, event);
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockProcessAddressEndpoint.expectedMessageCount(0);
		
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { patientId }));
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		mockGetQuotesEndpoint.expectedMessageCount(1);
		mockGetQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, patientId);
		final Integer[] quotationIds = new Integer[] { 1, 2 };
		mockGetQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(quotationIds));
		
		mockCancelQuotesEndpoint.expectedMessageCount(1);
		mockCancelQuotesEndpoint.expectedPropertyReceived("quotationIds", quotationIds);
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetQuotesEndpoint.assertIsSatisfied();
		mockCancelQuotesEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldNotCallCancelQuotationsForAVoidedPatientIfTheyHaveNone() throws Exception {
		final Integer patientId = 18;
		Event event = createEvent("patient", "1", PATIENT_UUID, "u");
		final Exchange exchange = new DefaultExchange(camelContext);
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		patient.setPatientVoided(true);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_ENTITY, patient);
		exchange.setProperty(PROP_EVENT, event);
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockProcessAddressEndpoint.expectedMessageCount(0);
		mockCancelQuotesEndpoint.expectedMessageCount(0);
		
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { patientId }));
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		mockGetQuotesEndpoint.expectedMessageCount(1);
		mockGetQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, patientId);
		mockGetQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetQuotesEndpoint.assertIsSatisfied();
		mockCancelQuotesEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
}
