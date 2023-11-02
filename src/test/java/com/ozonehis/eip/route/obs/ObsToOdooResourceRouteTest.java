package com.ozonehis.eip.route.obs;

import static com.ozonehis.eip.route.OdooTestConstants.APP_PROP_NAME_OBS_TO_ODOO_HANDLER;
import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_ODOO_RESOURCE;
import static com.ozonehis.eip.route.OdooTestConstants.URI_CONVERT_TO_CONCEPT_UUID;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_ODOO_RESOURCE;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNull;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

import com.ozonehis.eip.route.BaseOdooRouteTest;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/obs/" + ROUTE_ID_OBS_TO_ODOO_RESOURCE
        + ".xml")
@TestPropertySource(properties = "eip.watchedTables=obs")
@TestPropertySource(properties = "odoo.handler.route=odoo-prp-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=obs:obs")
@TestPropertySource(properties = "odoo.obs.concept.question.answer.mappings=" + ObsToOdooResourceRouteTest.CONCEPT_UUID_1
        + "#" + ObsToOdooResourceRouteTest.CONCEPT_UUID_A + "," + ObsToOdooResourceRouteTest.CONCEPT_SOURCE_2 + ":"
        + ObsToOdooResourceRouteTest.CONCEPT_CODE_2 + "#" + ObsToOdooResourceRouteTest.CONCEPT_UUID_B + "^"
        + ObsToOdooResourceRouteTest.CONCEPT_SOURCE_C + ":" + ObsToOdooResourceRouteTest.CONCEPT_CODE_C)
@TestPropertySource(properties = APP_PROP_NAME_OBS_TO_ODOO_HANDLER + "="
        + ObsToOdooResourceRouteTest.ROUTE_OBS_TO_ODOO_RES_HANDLER)
public class ObsToOdooResourceRouteTest extends BaseOdooRouteTest {
	
	private static final String TABLE = "obs";
	
	private static final String OBS_UUID = "obs-uuid-1";
	
	protected static final String CONCEPT_UUID_1 = "concept-uuid-1";
	
	protected static final String CONCEPT_UUID_2 = "concept-uuid-2";
	
	protected static final String CONCEPT_SOURCE_2 = "concept-source-2";
	
	protected static final String CONCEPT_CODE_2 = "concept-code-2";
	
	protected static final String CONCEPT_UUID_A = "concept-uuid-a";
	
	protected static final String CONCEPT_UUID_B = "concept-uuid-b";
	
	protected static final String CONCEPT_UUID_C = "concept-uuid-c";
	
	protected static final String CONCEPT_SOURCE_C = "concept-source-c";
	
	protected static final String CONCEPT_CODE_C = "concept-code-c";
	
	private static final String EX_PROP_OBS_QN_ANS_MAP = "obsQnAnsMap";
	
	private static final String OBS_QN_ANS_MAP_KEY = ROUTE_ID_OBS_TO_ODOO_RESOURCE + "-obsQnAnsMap";
	
	protected static final String ROUTE_OBS_TO_ODOO_RES_HANDLER = "obs-to-odoo-res-test-handler";
	
	@EndpointInject("mock:convert-to-concept-uuid-if-is-mapping")
	private MockEndpoint mockConvertToConceptUuidEndpoint;
	
	@EndpointInject("mock:" + ROUTE_OBS_TO_ODOO_RES_HANDLER)
	private MockEndpoint mockObsToOdooResHandlerEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockObsToOdooResHandlerEndpoint.reset();
		mockConvertToConceptUuidEndpoint.reset();
		AppContext.remove(OBS_QN_ANS_MAP_KEY);
		
		advise(ROUTE_ID_OBS_TO_ODOO_RESOURCE, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_CONVERT_TO_CONCEPT_UUID).skipSendToOriginalEndpoint()
				        .to(mockConvertToConceptUuidEndpoint);
				interceptSendToEndpoint("direct:" + ROUTE_OBS_TO_ODOO_RES_HANDLER).skipSendToOriginalEndpoint()
				        .to(mockObsToOdooResHandlerEndpoint);
			}
			
		});
		
		mockConvertToConceptUuidEndpoint.expectedMessageCount(5);
		mockConvertToConceptUuidEndpoint.expectedBodiesReceived(CONCEPT_UUID_1, CONCEPT_UUID_A,
		    CONCEPT_SOURCE_2 + ":" + CONCEPT_CODE_2, CONCEPT_UUID_B, CONCEPT_SOURCE_C + ":" + CONCEPT_CODE_C);
		mockConvertToConceptUuidEndpoint.whenExchangeReceived(3, e -> e.getIn().setBody(CONCEPT_UUID_2));
		mockConvertToConceptUuidEndpoint.whenExchangeReceived(5, e -> e.getIn().setBody(CONCEPT_UUID_C));
	}
	
	@After
	public void after() throws Exception {
		mockConvertToConceptUuidEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldProcessAnObsInsertEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(1);
		
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		mockConvertToConceptUuidEndpoint.assertIsSatisfied();
		Map<String, Set<Object>> obsQnAnsMap = exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP, Map.class);
		Assert.assertNotNull(obsQnAnsMap);
		Assert.assertEquals(1, obsQnAnsMap.get(CONCEPT_UUID_1).size());
		Assert.assertEquals(2, obsQnAnsMap.get(CONCEPT_UUID_2).size());
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_1).contains(CONCEPT_UUID_A));
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_2).contains(CONCEPT_UUID_B));
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_2).contains(CONCEPT_UUID_C));
	}

	@Test
	public void shouldSkipAnObsWithANullValue() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value",null);
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(0);

		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);

		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.DEBUG, "Skipping obs because it's value is null");
	}
	
	@Test
	public void shouldProcessAnObsUpdateEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "u");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(1);
		
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		Map<String, Set<Object>> obsQnAnsMap = exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP, Map.class);
		Assert.assertNotNull(obsQnAnsMap);
		Assert.assertEquals(1, obsQnAnsMap.get(CONCEPT_UUID_1).size());
		Assert.assertEquals(2, obsQnAnsMap.get(CONCEPT_UUID_2).size());
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_1).contains(CONCEPT_UUID_A));
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_2).contains(CONCEPT_UUID_B));
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_2).contains(CONCEPT_UUID_C));
	}
	
	@Test
	public void shouldSkipAnObsDeleteEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "d");
		exchange.setProperty(PROP_EVENT, event);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(0);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(0);
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP));
		assertMessageLogged(Level.DEBUG, "Skipping deleted obs");
	}
	
	@Test
	public void shouldSkipAnEventForAVoidedObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("voided", true));
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(0);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP));
		assertMessageLogged(Level.DEBUG, "Skipping voided obs");
	}
	
	@Test
	public void shouldUseTheObsQuestionAndAnswersMapFromTheEipContextCache() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		final String questionConceptUuid = "test-qn-concept";
		final String answerConceptUuid = "test-ans-concept";
		Map cachedObsQnAnsMap = singletonMap(questionConceptUuid, singleton(answerConceptUuid));
		AppContext.add(OBS_QN_ANS_MAP_KEY, cachedObsQnAnsMap);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(0);
		mockConvertToConceptUuidEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		Assert.assertEquals(cachedObsQnAnsMap, exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP, Map.class));
	}
	
	@Test
	public void shouldSkipAnEventForAnObsWithNonMonitoredQuestionConcept() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		obsResource.put("concept", singletonMap("uuid", "some-question-concept-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.DEBUG, "Skipping Obs because the question concept doesn't match any configured question");
	}
	
	@Test
	public void shouldSkipAnEventForAnObsWithNonMonitoredAnswerConcept() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", "some-answer-concept-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockObsToOdooResHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_ODOO_RESOURCE, exchange);
		
		mockObsToOdooResHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.DEBUG, "Skipping Obs because the answer concept doesn't match any configured answer");
	}
	
}
