package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "validation.concept=" + IsObsEncounterValidatedRuleRouteTest.VALIDATION_CONCEPT_UUID)
public class IsObsEncounterValidatedRuleRouteTest extends BasePrpRouteTest {
	
	private static final String ROUTE_ID = "is-obs-encounter-validated-rule";
	
	private static final String URI = "direct:" + ROUTE_ID;
	
	protected static final String VALIDATION_CONCEPT_UUID = "validation-concept-uuid";
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockFetchResourceEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-fetch-resource").skipSendToOriginalEndpoint()
				        .to(mockFetchResourceEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldSetBodyToTrueIfTheEncounterContainsTheValidationObs() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map encResource = new HashMap();
		encResource.put("obs",
		    asList(singletonMap("concept", singletonMap("uuid", "test-1")),
		        singletonMap("concept", singletonMap("uuid", VALIDATION_CONCEPT_UUID)),
		        singletonMap("concept", singletonMap("uuid", "test-2"))));
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.getIn().setBody(obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		Assert.assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToTrueIfTheEncounterContainsTheValidationObsIfItIsTheLastInTheList() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map encResource = new HashMap();
		encResource.put("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")),
		    singletonMap("concept", singletonMap("uuid", VALIDATION_CONCEPT_UUID))));
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.getIn().setBody(obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		Assert.assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToFalseIfTheEncounterDoesNotContainTheValidationObs() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map encResource = new HashMap();
		encResource.put("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")),
		    singletonMap("concept", singletonMap("uuid", "test-2"))));
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.getIn().setBody(obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		Assert.assertFalse(exchange.getIn().getBody(Boolean.class));
		
	}
	
	@Test
	public void shouldSetBodyToFalseIfNoEncounterIsFound() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.getIn().setBody(obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "No associated encounter found with uuid: " + encounterUuid);
		Assert.assertFalse(exchange.getIn().getBody(Boolean.class));
		
	}
	
	@Test
	public void shouldSetBodyToFalseForAnEncounterLessObs() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map encResource = new HashMap();
		encResource.put("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")),
		    singletonMap("concept", singletonMap("uuid", "test-2"))));
		exchange.getIn().setBody(new HashMap());
		mockFetchResourceEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		Assert.assertFalse(exchange.getIn().getBody(Boolean.class));
		assertMessageLogged(Level.INFO, "Obs has no encounter");
		
	}
	
}
