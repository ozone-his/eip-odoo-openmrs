package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_YES;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_IS_ENC_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_CONVERT_TO_CONCEPT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_IS_ENC_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "validation.concept=" + IsEncounterValidatedRouteTest.VALIDATION_CONCEPT_UUID)
public class IsEncounterValidatedRouteTest extends BasePrpRouteTest {
	
	protected static final String VALIDATION_CONCEPT_UUID = "validation-concept-uuid";
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@EndpointInject("mock:convert-to-concept-uuid-if-is-mapping")
	private MockEndpoint mockConvertToConceptUuidEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		mockConvertToConceptUuidEndpoint.reset();
		
		advise(ROUTE_ID_IS_ENC_VALIDATED, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:get-entity-by-uuid-from-openmrs").skipSendToOriginalEndpoint()
				        .to(mockFetchResourceEndpoint);
				interceptSendToEndpoint(URI_CONVERT_TO_CONCEPT_UUID).skipSendToOriginalEndpoint()
				        .to(mockConvertToConceptUuidEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldSetBodyToTrueIfTheEncounterContainsTheValidationObs() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map validationObs = new HashMap();
		validationObs.put("concept", singletonMap("uuid", VALIDATION_CONCEPT_UUID));
		validationObs.put("value", singletonMap("uuid", CONCEPT_UUID_YES));
		Map encResource = new HashMap();
		encResource.put("uuid", encounterUuid);
		encResource.put("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")), validationObs,
		    singletonMap("concept", singletonMap("uuid", "test-2"))));
		exchange.getIn().setBody(encounterUuid);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(1);
		mockConvertToConceptUuidEndpoint.expectedBodiesReceived(VALIDATION_CONCEPT_UUID);
		mockConvertToConceptUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(VALIDATION_CONCEPT_UUID));
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI_IS_ENC_VALIDATED, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToTrueIfTheEncounterContainsTheValidationObsIfItIsTheLastInTheList() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map validationObs = new HashMap();
		validationObs.put("concept", singletonMap("uuid", VALIDATION_CONCEPT_UUID));
		validationObs.put("value", singletonMap("uuid", CONCEPT_UUID_YES));
		Map encResource = new HashMap();
		encResource.put("uuid", encounterUuid);
		encResource.put("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")), validationObs));
		exchange.getIn().setBody(encounterUuid);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(1);
		mockConvertToConceptUuidEndpoint.expectedBodiesReceived(VALIDATION_CONCEPT_UUID);
		mockConvertToConceptUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(VALIDATION_CONCEPT_UUID));
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI_IS_ENC_VALIDATED, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToFalseIfTheEncounterContainsTheValidationObsWithValueNotSetToTrue() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map validationObs = new HashMap();
		validationObs.put("concept", singletonMap("uuid", VALIDATION_CONCEPT_UUID));
		validationObs.put("value", singletonMap("uuid", "another-concept-uuid"));
		Map encResource = new HashMap();
		encResource.put("uuid", encounterUuid);
		encResource.put("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")), validationObs,
		    singletonMap("concept", singletonMap("uuid", "test-2"))));
		exchange.getIn().setBody(encounterUuid);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(1);
		mockConvertToConceptUuidEndpoint.expectedBodiesReceived(VALIDATION_CONCEPT_UUID);
		mockConvertToConceptUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(VALIDATION_CONCEPT_UUID));
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI_IS_ENC_VALIDATED, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertFalse(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToFalseIfTheEncounterDoesNotContainTheValidationQuestionObs() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map validationObs = new HashMap();
		validationObs.put("concept", singletonMap("uuid", "another-concept-uid"));
		validationObs.put("value", singletonMap("uuid", CONCEPT_UUID_YES));
		Map encResource = new HashMap();
		encResource.put("uuid", encounterUuid);
		encResource.put("obs", asList(validationObs));
		exchange.getIn().setBody(encounterUuid);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(1);
		mockConvertToConceptUuidEndpoint.expectedBodiesReceived(VALIDATION_CONCEPT_UUID);
		mockConvertToConceptUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(VALIDATION_CONCEPT_UUID));
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI_IS_ENC_VALIDATED, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertFalse(exchange.getIn().getBody(Boolean.class));
		
	}
	
	@Test
	public void shouldSetBodyToFalseIfNoEncounterIsFound() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_IS_ENC_VALIDATED, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.WARN, "No encounter found with uuid: " + encounterUuid);
		assertFalse(exchange.getIn().getBody(Boolean.class));
		
	}
	
}
