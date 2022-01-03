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
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_ID_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ODOO_AUTH;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_ASSOCIATION_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_HANDLER;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "odoo.custom.table.resource.mappings=obs:obs,encounter:encounter")
public class OdooIntegrationEventListenerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-event-listener";
	
	@EndpointInject("mock:odoo-auth")
	private MockEndpoint mockAuthEndpoint;
	
	@EndpointInject("mock:odoo-entity-handler")
	private MockEndpoint mockEntityHandlerEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject("mock:odoo-patient-association-handler")
	private MockEndpoint mockPatientAssociationEndpoint;
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockAuthEndpoint.reset();
		mockEntityHandlerEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		mockPatientAssociationEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_ODOO_AUTH).skipSendToOriginalEndpoint().to(mockAuthEndpoint);
				interceptSendToEndpoint(URI_FETCH_RESOURCE).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-entity-handler").skipSendToOriginalEndpoint()
				        .to(mockEntityHandlerEndpoint);
				interceptSendToEndpoint(URI_PATIENT_HANDLER).skipSendToOriginalEndpoint().to(mockPatientHandlerEndpoint);
				interceptSendToEndpoint(URI_PATIENT_ASSOCIATION_HANDLER).skipSendToOriginalEndpoint()
				        .to(mockPatientAssociationEndpoint);
			}
		});
	}
	
	@Test
	public void shouldSkipSnapshotEvents() {
		Event event = createEvent("orders", "1", "some-uuid", "c");
		event.setSnapshot(true);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
	@Test
	public void shouldSkipNonMonitoredTables() {
		Event event = createEvent("visit", "1", "some_uuid", "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
	@Test
	public void shouldInvokeTheConfiguredObsHandler() throws Exception {
		final String obsUuid = "obs_uuid";
		Event event = createEvent("obs", "1", obsUuid, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", obsUuid);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "obs");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, obsUuid);
		final String obsJson = mapper.writeValueAsString(obsResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(obsJson));
		mockEntityHandlerEndpoint.expectedMessageCount(1);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockEntityHandlerEndpoint.assertIsSatisfied();
		Map map = exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP, Map.class);
		assertNotNull(map);
		assertEquals(6, map.size());
		assertEquals("patient", map.get("patient"));
		assertEquals("name", map.get("person_name"));
		assertEquals("address", map.get("person_address"));
		assertEquals("identifier", map.get("patient_identifier"));
		assertEquals("obs", map.get("obs"));
		assertEquals("encounter", map.get("encounter"));
	}
	
	@Test
	public void shouldProcessAnEventForAPatient() throws Exception {
		Event event = createEvent("patient", "5", PATIENT_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockEntityHandlerEndpoint.expectedMessageCount(0);
		mockPatientAssociationEndpoint.expectedMessageCount(0);
		mockPatientAssociationEndpoint.expectedMessageCount(0);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		Map patientResource = singletonMap("uuid", PATIENT_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockAuthEndpoint.assertIsSatisfied();
		mockEntityHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPatientAssociationEndpoint.assertIsSatisfied();
		assertEquals(patientResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPersonName() throws Exception {
		Event event = createEvent("person_name", "6", NAME_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPatientAssociationEndpoint.expectedMessageCount(1);
		mockPatientAssociationEndpoint.expectedPropertyReceived("patientAssociationName", "Person Name");
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockEntityHandlerEndpoint.expectedMessageCount(0);
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
		mockEntityHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPatientAssociationEndpoint.assertIsSatisfied();
		assertEquals(nameResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPersonAddress() throws Exception {
		Event event = createEvent("person_address", "7", ADDRESS_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPatientAssociationEndpoint.expectedMessageCount(1);
		mockPatientAssociationEndpoint.expectedPropertyReceived("patientAssociationName", "Person Address");
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockEntityHandlerEndpoint.expectedMessageCount(0);
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
		mockEntityHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPatientAssociationEndpoint.assertIsSatisfied();
		assertEquals(addressResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}
	
	@Test
	public void shouldProcessAnEventForAPatientIdentifier() throws Exception {
		Event event = createEvent("patient_identifier", "8", PATIENT_ID_UUID, "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		mockAuthEndpoint.expectedMessageCount(1);
		mockPatientAssociationEndpoint.expectedMessageCount(1);
		mockPatientAssociationEndpoint.expectedPropertyReceived("patientAssociationName", "Patient Identifier");
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockEntityHandlerEndpoint.expectedMessageCount(0);
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
		mockEntityHandlerEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockPatientAssociationEndpoint.assertIsSatisfied();
		assertEquals(idResource, exchange.getProperty(EX_PROP_ENTITY));
		assertNotNull(exchange.getProperty(EX_PROP_ODOO_USER_ID));
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_RESOURCE_MAP));
	}
	
}
