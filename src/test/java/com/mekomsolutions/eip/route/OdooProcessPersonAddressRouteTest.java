package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.component.entity.Patient;
import org.openmrs.eip.component.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;

public class OdooProcessPersonAddressRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-process-person-address";
	
	public static final String URI_PROCESS_ADDRESS = "direct:odoo-process-person-address";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_PREF_ADDRESS = "preferredAddress";
	
	public static final String EX_PROP_IS_SUB_RES = "isSubResource";
	
	public static final String EX_PROP_RES = "resourceName";
	
	public static final String EX_PROP_RES_ID = "resourceId";
	
	public static final String EX_PROP_SUB_RES = "subResourceName";
	
	public static final String EX_PROP_SUB_RES_ID = "subResourceId";
	
	public static final String EX_PROP_STATE_ID = "odooStateId";
	
	public static final String EX_PROP_COUNTRY_ID = "odooCountryId";
	
	public static final String EX_PROP_STATE = "stateName";
	
	public static final String EX_PROP_COUNTRY = "countryName";
	
	public static final String ADDRESS_UUID = "359022bf-4a58-4732-8cce-1e57f72f47b0";
	
	@EndpointInject("mock:odoo-get-state")
	private MockEndpoint mockGetStateEndpoint;
	
	@EndpointInject("mock:odoo-get-country")
	private MockEndpoint mockGetCountryEndpoint;
	
	@EndpointInject("mock:odoo-fetch-resource")
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Autowired
	private PatientRepository patientRepo;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockGetStateEndpoint.reset();
		mockGetCountryEndpoint.reset();
		
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUB_RES, true);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES, "person");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RES, "address");
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-get-state").skipSendToOriginalEndpoint().to(mockGetStateEndpoint);
				interceptSendToEndpoint("direct:odoo-get-country").skipSendToOriginalEndpoint().to(mockGetCountryEndpoint);
				interceptSendToEndpoint("direct:odoo-fetch-resource").skipSendToOriginalEndpoint()
				        .to(mockFetchResourceEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldProcessStateAndCountryIfTheyExistOnTheAddress() throws Exception {
		final Integer stateId = 6;
		final Integer countryId = 8;
		final String state = "TX";
		final String country = "US";
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_PREF_ADDRESS, singletonMap("uuid", ADDRESS_UUID));
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RES_ID, ADDRESS_UUID);
		Map<String, Object> addressResource = new HashMap();
		addressResource.put("stateProvince", state);
		addressResource.put("country", country);
		final String addressJson = new ObjectMapper().writeValueAsString(addressResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(addressJson));
		
		mockGetStateEndpoint.expectedMessageCount(1);
		mockGetStateEndpoint.expectedPropertyReceived(EX_PROP_STATE, state);
		mockGetStateEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { stateId }));
		
		mockGetCountryEndpoint.expectedMessageCount(1);
		mockGetCountryEndpoint.expectedPropertyReceived(EX_PROP_COUNTRY, country);
		mockGetCountryEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { countryId }));
		
		producerTemplate.send(URI_PROCESS_ADDRESS, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockGetStateEndpoint.assertIsSatisfied();
		mockGetCountryEndpoint.assertIsSatisfied();
		assertEquals(stateId, exchange.getProperty(EX_PROP_STATE_ID));
		assertEquals(countryId, exchange.getProperty(EX_PROP_COUNTRY_ID));
	}
	
	@Test
	public void shouldNotSetStateOrCountryIfTheyExistOnTheAddressAndHaveNoMuchInOdoo() throws Exception {
		final String state = "TX";
		final String country = "US";
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_PREF_ADDRESS, singletonMap("uuid", ADDRESS_UUID));
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RES_ID, ADDRESS_UUID);
		Map<String, Object> addressResource = new HashMap();
		addressResource.put("stateProvince", state);
		addressResource.put("country", country);
		final String addressJson = new ObjectMapper().writeValueAsString(addressResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(addressJson));
		
		mockGetStateEndpoint.expectedMessageCount(1);
		mockGetStateEndpoint.expectedPropertyReceived(EX_PROP_STATE, state);
		mockGetStateEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		mockGetCountryEndpoint.expectedMessageCount(1);
		mockGetCountryEndpoint.expectedPropertyReceived(EX_PROP_COUNTRY, country);
		mockGetCountryEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		
		producerTemplate.send(URI_PROCESS_ADDRESS, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockGetStateEndpoint.assertIsSatisfied();
		mockGetCountryEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.WARN, "No state found in odoo matching name: " + state);
		assertMessageLogged(Level.WARN, "No country found in odoo matching name: " + country);
	}
	
	@Test
	public void shouldFailIfMultipleStatesAreFoundInOdooMatchingTheStateUuid() throws Exception {
		final String state = "TX";
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_PREF_ADDRESS, singletonMap("uuid", ADDRESS_UUID));
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RES_ID, ADDRESS_UUID);
		final String addressJson = new ObjectMapper().writeValueAsString(singletonMap("stateProvince", state));
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(addressJson));
		
		mockGetStateEndpoint.expectedMessageCount(1);
		mockGetStateEndpoint.expectedPropertyReceived(EX_PROP_STATE, state);
		mockGetStateEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { 6, 7 }));
		
		producerTemplate.send(URI_PROCESS_ADDRESS, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockGetStateEndpoint.assertIsSatisfied();
		mockGetCountryEndpoint.assertIsSatisfied();
		assertEquals("Found 2 states in odoo matching name: " + state, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfMultipleCountriesAreFoundInOdooMatchingTheCountryUuid() throws Exception {
		final Integer stateId = 6;
		final String state = "TX";
		final String country = "US";
		Patient patient = patientRepo.findByUuid(PATIENT_UUID);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_PATIENT, patient);
		exchange.setProperty(EX_PROP_PREF_ADDRESS, singletonMap("uuid", ADDRESS_UUID));
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_ID, PATIENT_UUID);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_SUB_RES_ID, ADDRESS_UUID);
		Map<String, Object> addressResource = new HashMap();
		addressResource.put("stateProvince", state);
		addressResource.put("country", country);
		final String addressJson = new ObjectMapper().writeValueAsString(addressResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(addressJson));
		
		mockGetStateEndpoint.expectedMessageCount(1);
		mockGetStateEndpoint.expectedPropertyReceived(EX_PROP_STATE, state);
		mockGetStateEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { stateId }));
		
		mockGetCountryEndpoint.expectedMessageCount(1);
		mockGetCountryEndpoint.expectedPropertyReceived(EX_PROP_COUNTRY, country);
		mockGetCountryEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] { 8, 9 }));
		
		producerTemplate.send(URI_PROCESS_ADDRESS, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockGetStateEndpoint.assertIsSatisfied();
		mockGetCountryEndpoint.assertIsSatisfied();
		assertEquals(stateId, exchange.getProperty(EX_PROP_STATE_ID));
		assertEquals("Found 2 countries in odoo matching name: " + country, getErrorMessage(exchange));
	}
	
}
