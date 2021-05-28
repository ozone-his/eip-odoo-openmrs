package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PERSON_NAME_ADDRESS_HANDLER;
import static java.util.Collections.singletonMap;

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

public class OdooPersonNameAndAddressHandlerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-person-name-and-address-handler";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_FETCH_RESOURCE).skipSendToOriginalEndpoint().to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-patient-handler").skipSendToOriginalEndpoint()
				        .to(mockPatientHandlerEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldLoadThePatientWhenProcessingPersonName() throws Exception {
		final String personUuid = "person-uuid";
		Map personResource = singletonMap("uuid", personUuid);
		Map nameResource = new HashMap();
		nameResource.put("uuid", "name-uuid");
		nameResource.put("person", personResource);
		Map patientResource = new HashMap();
		patientResource.put("uuid", personUuid);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, nameResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_PERSON_NAME_ADDRESS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldLoadThePatientWhenProcessingPersonAddress() throws Exception {
		final String personUuid = "person-uuid";
		Map personResource = singletonMap("uuid", personUuid);
		Map addressResource = new HashMap();
		addressResource.put("uuid", "address-uuid");
		addressResource.put("person", personResource);
		Map patientResource = new HashMap();
		patientResource.put("uuid", personUuid);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, addressResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_PERSON_NAME_ADDRESS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfNoPatientIsFound() throws Exception {
		final String badPersonUuid = "bad-uuid";
		Map personResource = singletonMap("uuid", badPersonUuid);
		Map addressResource = new HashMap();
		addressResource.put("uuid", "address-uuid");
		addressResource.put("person", personResource);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, addressResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PERSON_NAME_ADDRESS_HANDLER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.WARN, "No associated patient found with uuid: " + badPersonUuid);
	}
	
}
