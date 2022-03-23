package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENC;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_QN_CONCEPT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ENC_CONTAINS_OBS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.Test;

public class GetObsByQuestionInEncounterRouteTest extends BaseOdooRouteTest {
	
	@Test
	public void shouldSetBodyToTheMatchingObsIfTheEncounterHasIt() {
		final String qnUuid = "question-concept-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map expectedObs = singletonMap("concept", singletonMap("uuid", qnUuid));
		Map encResource = singletonMap("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")), expectedObs,
		    singletonMap("concept", singletonMap("uuid", "test-2"))));
		exchange.setProperty(EX_PROP_ENC, encResource);
		exchange.setProperty(EX_PROP_QN_CONCEPT_UUID, qnUuid);
		
		producerTemplate.send(URI_ENC_CONTAINS_OBS, exchange);
		
		assertEquals(expectedObs, exchange.getIn().getBody());
	}
	
	@Test
	public void shouldSetBodyToTheMatchingObsIfTheEncounterHasItAndIsTheLastInTheList() {
		final String qnUuid = "question-concept-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map expectedObs = singletonMap("concept", singletonMap("uuid", qnUuid));
		Map encResource = singletonMap("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")), expectedObs));
		exchange.setProperty(EX_PROP_ENC, encResource);
		exchange.setProperty(EX_PROP_QN_CONCEPT_UUID, qnUuid);
		
		producerTemplate.send(URI_ENC_CONTAINS_OBS, exchange);
		
		assertEquals(expectedObs, exchange.getIn().getBody());
	}
	
	@Test
	public void shouldSetBodyToNullIfTheEncounterDoesNotHaveIt() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map encResource = singletonMap("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1"))));
		exchange.setProperty(EX_PROP_ENC, encResource);
		exchange.setProperty(EX_PROP_QN_CONCEPT_UUID, "question-concept-uuid");
		
		producerTemplate.send(URI_ENC_CONTAINS_OBS, exchange);
		
		assertNull(exchange.getIn().getBody());
		
	}
	
	@Test
	public void shouldIgnoredVoidedObs() {
		final String qnUuid = "question-concept-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map expectedObs = new HashMap();
		expectedObs.put("concept", singletonMap("uuid", qnUuid));
		expectedObs.put("uuid", qnUuid);
		expectedObs.put("voided", true);
		Map encResource = singletonMap("obs", asList(expectedObs));
		exchange.setProperty(EX_PROP_ENC, encResource);
		exchange.setProperty(EX_PROP_QN_CONCEPT_UUID, qnUuid);
		
		producerTemplate.send(URI_ENC_CONTAINS_OBS, exchange);
		
		assertNull(exchange.getIn().getBody());
	}
	
}
