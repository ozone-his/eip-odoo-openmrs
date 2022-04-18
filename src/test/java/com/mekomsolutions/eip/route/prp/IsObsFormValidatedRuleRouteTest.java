package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_FORM_VALIDATED_RULE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_IS_ENC_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_FORM_VALIDATED_RULE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_IS_ENC_VALIDATED;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;

public class IsObsFormValidatedRuleRouteTest extends BasePrpRouteTest {
	
	@EndpointInject("mock:" + ROUTE_ID_IS_ENC_VALIDATED)
	private MockEndpoint mockIsEncValidatedEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockIsEncValidatedEndpoint.reset();
		
		advise(ROUTE_ID_FORM_VALIDATED_RULE, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_IS_ENC_VALIDATED).skipSendToOriginalEndpoint().to(mockIsEncValidatedEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldSetBodyToFalseForAnEncounterLessObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(new HashMap());
		mockIsEncValidatedEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_FORM_VALIDATED_RULE, exchange);
		
		mockIsEncValidatedEndpoint.assertIsSatisfied();
		Assert.assertFalse(exchange.getIn().getBody(Boolean.class));
		assertMessageLogged(Level.INFO, "Obs has no encounter");
	}
	
	@Test
	public void shouldCallIsEncounterValidatedRouteAndSetBodyToTrue() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(singletonMap("encounter", singletonMap("uuid", encounterUuid)));
		mockIsEncValidatedEndpoint.expectedMessageCount(1);
		mockIsEncValidatedEndpoint.expectedBodiesReceived(encounterUuid);
		mockIsEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		producerTemplate.send(URI_FORM_VALIDATED_RULE, exchange);
		
		mockIsEncValidatedEndpoint.assertIsSatisfied();
		assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
	@Test
	public void shouldCallIsEncounterValidatedRouteAndSetBodyToFalse() throws Exception {
		final String encounterUuid = "enc-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(singletonMap("encounter", singletonMap("uuid", encounterUuid)));
		mockIsEncValidatedEndpoint.expectedMessageCount(1);
		mockIsEncValidatedEndpoint.expectedBodiesReceived(encounterUuid);
		mockIsEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_FORM_VALIDATED_RULE, exchange);
		
		mockIsEncValidatedEndpoint.assertIsSatisfied();
		assertFalse(exchange.getIn().getBody(Boolean.class));
	}
	
}
