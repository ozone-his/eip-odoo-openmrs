package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.ADDRESS_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_USER_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_SUB_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_SUB_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_TABLE_RESOURCE_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.LISTENER_URI;
import static com.mekomsolutions.eip.route.OdooTestConstants.NAME_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_1;
import static com.mekomsolutions.eip.route.OdooTestConstants.ORDER_UUID_2;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_ID_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ODOO_AUTH;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ORDER_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PERSON_NAME_ADDRESS_HANDLER;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;

public class OdooIntegrationEventListenerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-event-listener";
	
	@EndpointInject("mock:odoo-auth")
	private MockEndpoint mockAuthEndpoint;
	
	@EndpointInject("mock:odoo-order-handler")
	private MockEndpoint mockOrderHandlerEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject("mock:odoo-person-name-and-address-handler")
	private MockEndpoint mockPersonNameAndAddressEndpoint;
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockAuthEndpoint.reset();
		mockOrderHandlerEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		mockPersonNameAndAddressEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_ODOO_AUTH).skipSendToOriginalEndpoint().to(mockAuthEndpoint);
				interceptSendToEndpoint(URI_FETCH_RESOURCE).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
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
		
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
	@Test
	public void shouldSkipNonMonitoredTables() throws Exception {
		Event event = createEvent("visit", "1", "some_uuid", "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
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
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "order");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, ORDER_UUID_1);
		Map orderResource = singletonMap("action", "NEW");
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		assertEquals(orderResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
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
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "order");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, ORDER_UUID_2);
		Map orderResource = singletonMap("action", "NEW");
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		assertEquals(orderResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
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
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "order");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, ORDER_UUID_1);
		Map orderResource = singletonMap("action", "NEW");
		final String orderJson = mapper.writeValueAsString(orderResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(orderJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		assertEquals(orderResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
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
		mockPersonNameAndAddressEndpoint.expectedMessageCount(0);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		Map patientResource = singletonMap("uuid", PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		assertEquals(patientResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPersonName() throws Exception {
		Event event = createEvent("person_name", "1", NAME_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(1);
		mockPersonNameAndAddressEndpoint.expectedPropertyReceived("patientAssociationName", "Person Name");
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockOrderHandlerEndpoint.expectedMessageCount(0);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, true);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "person");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RESOURCE_NAME, "name");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RESOURCE_ID, NAME_UUID);
		Map nameResource = singletonMap("uuid", NAME_UUID);
		final String nameJson = mapper.writeValueAsString(nameResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(nameJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		assertEquals(nameResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPersonAddress() throws Exception {
		Event event = createEvent("person_address", "1", ADDRESS_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPersonNameAndAddressEndpoint.expectedMessageCount(1);
		mockPersonNameAndAddressEndpoint.expectedPropertyReceived("patientAssociationName", "Person Address");
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockOrderHandlerEndpoint.expectedMessageCount(0);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, true);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "person");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RESOURCE_NAME, "address");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RESOURCE_ID, ADDRESS_UUID);
		Map addressResource = singletonMap("uuid", ADDRESS_UUID);
		final String addressJson = mapper.writeValueAsString(addressResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(addressJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockOrderHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPersonNameAndAddressEndpoint.assertIsSatisfied();
		assertEquals(addressResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}

    @Test
    public void shouldProcessAnEventForAPatientIdentifier() throws Exception {
        Event event = createEvent("patient_identifier", "1", PATIENT_ID_UUID, "c");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        mockAuthEndpoint.expectedMessageCount(1);
        mockPersonNameAndAddressEndpoint.expectedMessageCount(1);
        mockPersonNameAndAddressEndpoint.expectedPropertyReceived("patientAssociationName", "Patient Identifier");
        mockPatientHandlerEndpoint.expectedMessageCount(0);
        mockOrderHandlerEndpoint.expectedMessageCount(0);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, true);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RESOURCE_NAME, "identifier");
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RESOURCE_ID, PATIENT_ID_UUID);
        Map idResource = singletonMap("uuid", PATIENT_ID_UUID);
        final String idJson = mapper.writeValueAsString(idResource);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(idJson));

        producerTemplate.send(LISTENER_URI, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        mockAuthEndpoint.assertIsSatisfied();
        mockOrderHandlerEndpoint.assertIsSatisfied();
        mockPatientHandlerEndpoint.assertIsSatisfied();
        mockPersonNameAndAddressEndpoint.assertIsSatisfied();
        assertEquals(idResource, exchange.getProperty(EX_PROP_ENTITY));
        assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
        assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
    }
	
}
