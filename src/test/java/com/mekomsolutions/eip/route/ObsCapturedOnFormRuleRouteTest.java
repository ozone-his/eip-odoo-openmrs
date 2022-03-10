package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_CAPTURED_ON_FORM;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;

public class ObsCapturedOnFormRuleRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "obs-captured-on-form-rule";
	
	public static final String EX_PROP_OBS = "obs";
	
	public static final String EX_PROP_FORM_UUID = "formUuid";
	
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
	public void shouldSetBodyToTrueIfTheObsWasRecordedOnTheFormWithTheSpecifiedUuid() throws Exception {
		final String encounterUuid = "enc-uuid";
		final String formUuid = "form-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map encResource = singletonMap("form", singletonMap("uuid", formUuid));
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.setProperty(EX_PROP_OBS, obsResource);
		exchange.setProperty(EX_PROP_FORM_UUID, formUuid);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI_OBS_CAPTURED_ON_FORM, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		Assert.assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToFalseIfTheObsWasRecordedOnAFormWithADifferentUuid() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map encResource = singletonMap("form", singletonMap("uuid", "another-form-uuid"));
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.setProperty(EX_PROP_OBS, obsResource);
		exchange.setProperty(EX_PROP_FORM_UUID, "form-uuid");
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		final String encJson = mapper.writeValueAsString(encResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));
		
		producerTemplate.send(URI_OBS_CAPTURED_ON_FORM, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertFalse(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldSetBodyToFalseForAFormLessEncounter() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.setProperty(EX_PROP_OBS, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(emptyMap()));
		
		producerTemplate.send(URI_OBS_CAPTURED_ON_FORM, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "Obs encounter does not belong to any form");
		assertFalse(exchange.getIn().getBody(Boolean.class));
		
	}
	
	@Test
	public void shouldSetBodyToFalseForAnEncounterLessObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_OBS, emptyMap());
		mockFetchResourceEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_CAPTURED_ON_FORM, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertFalse(exchange.getIn().getBody(Boolean.class));
		assertMessageLogged(Level.INFO, "Obs does not belong to any encounter");
		
	}
	
	@Test
	public void shouldFailIfNoEncounterIsFoundMatchingTheEncounterUuid() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(encounterUuid);
		Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
		exchange.setProperty(EX_PROP_OBS, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encounterUuid);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_OBS_CAPTURED_ON_FORM, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		assertEquals("No encounter found with uuid: " + encounterUuid, getErrorMessage(exchange));
		
	}
	
}
