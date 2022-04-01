package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_PATIENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_HANDLER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PERSON_HANDLER;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;

public class OdooPersonHandlerRouteTest extends BasePrpRouteTest {
	
	protected static final String ROUTE_ID = "odoo-person-handler";
	
	protected static final String PERSON_UUID = "person-uuid";
	
	protected static final String TABLE = "person";
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PERSON_UUID);
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
				interceptSendToEndpoint(URI_PATIENT_HANDLER).skipSendToOriginalEndpoint().to(mockPatientHandlerEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldCallPersonHandlerForAPersonEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, Collections.singletonMap("uuid", PERSON_UUID));
		Map patientResource = new HashMap();
		patientResource.put("uuid", PERSON_UUID);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_PERSON_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldNotCallPersonHandlerForAPersonEventWithNoAssociatedPatientRecord() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, Collections.singletonMap("uuid", PERSON_UUID));
		Map patientResource = new HashMap();
		patientResource.put("uuid", PERSON_UUID);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PERSON_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "No patient record found associated to person with uuid: " + PERSON_UUID);
	}
	
}
