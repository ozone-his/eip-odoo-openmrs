package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_PATIENT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_WRITE;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
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
import org.junit.Ignore;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "openmrs.identifier.type.uuid=" + OdooPatientHandlerRouteTest.ID_TYPE_UUID)
public class OdooPatientHandlerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-patient-handler";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_CREATE_CUSTOMER = "createCustomerIfNotExist";
	
	public static final String EX_PROP_CUSTOM_DATA = "customPatientData";
	
	public static final String ID_TYPE_UUID = "8d79403a-c2cc-11de-8d13-0010c6dffd0f";
	
	public static final String ID_TYPE_ID_KEY = "odoo-patient-handler-idTypeId";
	
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
	
	@EndpointInject("mock:odoo-get-custom-customer-data")
	private MockEndpoint mockGetCustomDataEndpoint;
	
	@Before
	public void setup() throws Exception {
		AppContext.remove(ID_TYPE_ID_KEY);
		mockGetCustomerEndpoint.reset();
		mockManageCustomerEndpoint.reset();
		mockProcessAddressEndpoint.reset();
		mockGetQuotesEndpoint.reset();
		mockCancelQuotesEndpoint.reset();
		mockGetCustomDataEndpoint.reset();
		mockGetCustomerEndpoint.expectedMessageCount(1);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-get-customer").skipSendToOriginalEndpoint().to(mockGetCustomerEndpoint);
				interceptSendToEndpoint("direct:odoo-get-custom-customer-data").skipSendToOriginalEndpoint()
				        .to(mockGetCustomDataEndpoint);
				interceptSendToEndpoint("direct:odoo-manage-customer").skipSendToOriginalEndpoint()
				        .to(mockManageCustomerEndpoint);
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
	public void shouldFailIfThePatientHasMultipleCustomerRecordsInOdoo() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = singletonMap("uuid", PATIENT_UUID);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { 1, 2 }));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertEquals("Found 2 existing customers in odoo with ref: " + PATIENT_UUID, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldSkipPatientsWithNoCustomerRecordAndTheEventIsForAPatient() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.DEBUG, "Patient has no customer record in Odoo, there is nothing to be processed");
	}
	
	@Test
	public void shouldSkipPatientsWithNoCustomerRecordAndTheEventIsForAPersonName() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.DEBUG, "Patient has no customer record in Odoo, there is nothing to be processed");
	}
	
	@Test
	public void shouldSkipPatientsWithNoCustomerRecordAndTheEventIsForAPersonAddress() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		assertMessageLogged(Level.DEBUG, "Patient has no customer record in Odoo, there is nothing to be processed");
	}
	
	@Test
	public void shouldAddPatientToOdooIfTheyDoNotExistAddCreateCustomerPropIsSetToTrue() throws Exception {
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Map personResource = new HashMap();
		final String name = "Test User";
		personResource.put("display", name);
		Map addressResource = new HashMap();
		addressResource.put("address1", "25 Ocean drive");
		personResource.put("preferredAddress", addressResource);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		patientResource.put("person", personResource);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CREATE_CUSTOMER, true);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		mockProcessAddressEndpoint.expectedMessageCount(1);
		mockProcessAddressEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		final int countryId = 4;
		final int stateId = 5;
		final int patientId = 12;
		mockProcessAddressEndpoint.whenAnyExchangeReceived(e -> {
			e.setProperty("odooCountryId", countryId);
			e.setProperty("odooStateId", stateId);
		});
		
		mockGetCustomDataEndpoint.expectedMessageCount(1);
		mockGetCustomDataEndpoint.expectedPropertyReceived(EX_PROP_CUSTOM_DATA, new HashMap());
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patientResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdentifier", "12345");
		mockManageCustomerEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("odooCountryId", countryId);
		mockManageCustomerEndpoint.expectedPropertyReceived("odooStateId", stateId);
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody(patientId);
		});
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldAddPatientWithoutAddressToOdooIfTheyDoNotExistAndCreateCustomerPropIsSetToTrue() throws Exception {
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Map personResource = new HashMap();
		final String name = "Test User";
		personResource.put("display", name);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		patientResource.put("person", personResource);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CREATE_CUSTOMER, true);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		mockGetCustomDataEndpoint.expectedMessageCount(1);
		mockGetCustomDataEndpoint.expectedPropertyReceived(EX_PROP_CUSTOM_DATA, new HashMap());
		mockProcessAddressEndpoint.expectedMessageCount(0);
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patientResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdentifier", "12345");
		final int patientId = 12;
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientId));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetCustomDataEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
		assertMessageLogged(Level.DEBUG, "Patient has no address");
	}
	
	@Test
	public void shouldUpdatePatientInOdooIfTheyAlreadyExist() throws Exception {
		final int patientId = 16;
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Map personResource = new HashMap();
		final String name = "Another Test User";
		personResource.put("display", name);
		Map addressResource = new HashMap();
		addressResource.put("address1s", "26 Oceanic drives");
		personResource.put("preferredAddress", addressResource);
		personResource.put("display", name);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		patientResource.put("person", personResource);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomDataEndpoint.expectedMessageCount(1);
		mockGetCustomDataEndpoint.expectedPropertyReceived(EX_PROP_CUSTOM_DATA, new HashMap());
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { patientId }));
		
		final int countryId = 4;
		final int stateId = 5;
		mockProcessAddressEndpoint.expectedMessageCount(1);
		mockProcessAddressEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		mockProcessAddressEndpoint.whenAnyExchangeReceived(e -> {
			e.setProperty("odooCountryId", countryId);
			e.setProperty("odooStateId", stateId);
		});
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patientResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdentifier", "12345");
		mockManageCustomerEndpoint.expectedPropertyReceived("preferredAddress", addressResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("odooCountryId", countryId);
		mockManageCustomerEndpoint.expectedPropertyReceived("odooStateId", stateId);
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientId));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetCustomDataEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldNotAddAVoidedPatientToOdooIfTheyDoNotExist() throws Exception {
		Event event = createEvent("orders", "1", "some-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("patientVoided", true);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
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
		Map patientResource = singletonMap("patientVoided", true);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(PROP_EVENT, event);
		mockProcessAddressEndpoint.expectedMessageCount(0);
		mockGetCustomDataEndpoint.expectedMessageCount(0);
		
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
		
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetQuotesEndpoint.assertIsSatisfied();
		mockCancelQuotesEndpoint.assertIsSatisfied();
		mockGetCustomDataEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	@Ignore
	public void shouldInactiveCustomerAndCancelTheirQuotationsForADeletedPatientThatExistsInOdoo() throws Exception {
		//TODO support deleted patient
		final Integer patientId = 18;
		Event event = createEvent("patient", "1", PATIENT_UUID, "d");
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
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
		Map patientResource = singletonMap("patientVoided", true);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(PROP_EVENT, event);
		mockProcessAddressEndpoint.expectedMessageCount(0);
		mockCancelQuotesEndpoint.expectedMessageCount(0);
		
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { patientId }));
		
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_WRITE);
		
		mockGetQuotesEndpoint.expectedMessageCount(1);
		mockGetQuotesEndpoint.expectedPropertyReceived(EX_PROP_ODOO_PATIENT_ID, patientId);
		mockGetQuotesEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetQuotesEndpoint.assertIsSatisfied();
		mockCancelQuotesEndpoint.assertIsSatisfied();
		assertTrue(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
	}
	
	@Test
	public void shouldFailIfNoPatientIdentifierTypeIsFoundMatchingTheUuid() throws Exception {
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		producerTemplate.send("sql:UPDATE patient_identifier_type set uuid = 'other' WHERE uuid = '" + ID_TYPE_UUID
		        + "'?dataSource=openmrsDataSource",
		    exchange);
		exchange.setProperty(EX_PROP_PATIENT, new HashMap());
		exchange.setProperty(EX_PROP_CREATE_CUSTOMER, true);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		mockProcessAddressEndpoint.expectedMessageCount(0);
		mockManageCustomerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockGetCustomerEndpoint.assertIsSatisfied();
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		assertEquals("No patient identifier type found with uuid: " + ID_TYPE_UUID, getErrorMessage(exchange));
	}
	
}
