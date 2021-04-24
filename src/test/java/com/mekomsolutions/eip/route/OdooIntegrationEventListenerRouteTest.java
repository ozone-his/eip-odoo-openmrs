package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.ADDRESS_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_USER_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_TABLE_REPO_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.LISTENER_URI;
import static com.mekomsolutions.eip.route.OdooTestConstants.NAME_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_1;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_2;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ODOO_AUTH;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ORDER_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PERSON_NAME_ADDRESS_HANDLER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.component.entity.DrugOrder;
import org.openmrs.eip.component.entity.Order;
import org.openmrs.eip.component.entity.Patient;
import org.openmrs.eip.component.entity.PersonAddress;
import org.openmrs.eip.component.entity.PersonName;
import org.openmrs.eip.component.entity.TestOrder;
import org.openmrs.eip.component.repository.OrderRepository;
import org.openmrs.eip.component.repository.PatientRepository;
import org.openmrs.eip.component.repository.PersonAddressRepository;
import org.openmrs.eip.component.repository.PersonNameRepository;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@TestPropertySource(properties = "camel.springboot.route-filter-include-pattern=direct:odoo-event-listener")
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public class OdooIntegrationEventListenerRouteTest extends BaseWatcherRouteTest {
	
	private static final String ROUTE_ID = "odoo-event-listener";
	
	@EndpointInject("mock:odoo-auth")
	private MockEndpoint mockAuthEndpoint;
	
	@EndpointInject("mock:odoo-order-handler")
	private MockEndpoint mockOrderHandlerEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject("mock:odoo-person-name-and-address-handler")
	private MockEndpoint mockPersonNameAndAddressEndpoint;
	
	@Autowired
	private OrderRepository orderRepo;
	
	@Autowired
	private PatientRepository patientRepo;
	
	@Autowired
	private PersonNameRepository nameRepo;
	
	@Autowired
	private PersonAddressRepository addressRepo;
	
	@Before
	public void setup() throws Exception {
		mockErrorHandlerEndpoint.reset();
		mockAuthEndpoint.reset();
		mockOrderHandlerEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		mockPersonNameAndAddressEndpoint.reset();
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_ODOO_AUTH).skipSendToOriginalEndpoint().to(mockAuthEndpoint);
				interceptSendToEndpoint(URI_ORDER_HANDLER).skipSendToOriginalEndpoint().to(mockOrderHandlerEndpoint);
				interceptSendToEndpoint(URI_PATIENT_HANDLER).skipSendToOriginalEndpoint().to(mockPatientHandlerEndpoint);
				interceptSendToEndpoint(URI_PERSON_NAME_ADDRESS_HANDLER).skipSendToOriginalEndpoint()
				        .to(mockPersonNameAndAddressEndpoint);
			}
		});
	}
	
	@Test
	public void shouldSkipSnapshotEvents() throws Exception {
		Event event = createEvent("orders", "1", "some-uuid", "c");
		event.setSnapshot(true);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
	@Test
	public void shouldSkipNonMonitoredTables() throws Exception {
		Event event = createEvent("visit", "1", "some_uuid", "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
	@Test
	public void shouldProcessAnEventForAnOrder() throws Exception {
		Event event = createEvent("orders", "1", ORDER_UUID_1, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockOrderHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(0);
		Order expectedOrder = orderRepo.findByUuid(ORDER_UUID_1);
		assertNotNull(expectedOrder);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertEquals(expectedOrder, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForADrugOrder() throws Exception {
		Event event = createEvent("drug_order", "2", ORDER_UUID_2, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockOrderHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(0);
		Order expectedOrder = orderRepo.findByUuid(ORDER_UUID_2);
		assertNotNull(expectedOrder);
		assertTrue(expectedOrder instanceof DrugOrder);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertEquals(expectedOrder, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForATestOrder() throws Exception {
		Event event = createEvent("test_order", "1", ORDER_UUID_1, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockOrderHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(0);
		Order expectedOrder = orderRepo.findByUuid(ORDER_UUID_1);
		assertNotNull(expectedOrder);
		assertTrue(expectedOrder instanceof TestOrder);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertEquals(expectedOrder, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPatient() throws Exception {
		Event event = createEvent("patient", "1", PATIENT_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockOrderHandlerEndpoint.expectedMessageCount(0);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(0);
		Patient expectedPatient = patientRepo.findByUuid(PATIENT_UUID);
		assertNotNull(expectedPatient);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertEquals(expectedPatient, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPersonName() throws Exception {
		Event event = createEvent("person_name", "1", NAME_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockOrderHandlerEndpoint.expectedMessageCount(0);
		PersonName expectedName = nameRepo.findByUuid(NAME_UUID);
		assertNotNull(expectedName);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertEquals(expectedName, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPersonAddress() throws Exception {
		Event event = createEvent("person_address", "1", ADDRESS_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockOrderHandlerEndpoint.expectedMessageCount(0);
		PersonAddress expectedAddress = addressRepo.findByUuid(ADDRESS_UUID);
		assertNotNull(expectedAddress);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertEquals(expectedAddress, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
	}
	
}
