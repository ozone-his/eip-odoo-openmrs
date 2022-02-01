package com.mekomsolutions.eip.route.obs;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_HANDLER;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNull;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.BaseOdooRouteTest;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/obs/odoo-obs-to-customer.xml")
@TestPropertySource(properties = "eip.watchedTables=obs")
@TestPropertySource(properties = "odoo.handler.route=odoo-prp-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=obs:obs")
@TestPropertySource(properties = "odoo.obs.concept.question.answer.mappings=" + OdooObsToCustomerRouteTest.CONCEPT_UUID_1
        + ":" + OdooObsToCustomerRouteTest.CONCEPT_UUID_A + "," + OdooObsToCustomerRouteTest.CONCEPT_UUID_2 + ":"
        + OdooObsToCustomerRouteTest.CONCEPT_UUID_B + "^" + OdooObsToCustomerRouteTest.CONCEPT_UUID_C)
public class OdooObsToCustomerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-obs-to-customer";
	
	private static final String URI_TEST_RULE = "mock:test-rule";
	
	private static final String TABLE = "obs";
	
	private static final String PATIENT = "patient";
	
	private static final String OBS_UUID = "obs-uuid-1";
	
	private static final String PATIENT_UUID = "patient-uuid";
	
	protected static final String CONCEPT_UUID_1 = "concept-uuid-1";
	
	protected static final String CONCEPT_UUID_2 = "concept-uuid-2";
	
	protected static final String CONCEPT_UUID_A = "concept-uuid-a";
	
	protected static final String CONCEPT_UUID_B = "concept-uuid-b";
	
	protected static final String CONCEPT_UUID_C = "concept-uuid-c";
	
	private static final String EX_PROP_OBS_QN_ANS_MAP = "obsQnAnsMap";
	
	public static final String EX_PROP_CREATE_IF_NOT_EXISTS = "createCustomerIfNotExist";
	
	private static final String EX_PROP_PATIENT = PATIENT;
	
	private static final String OBS_QN_ANS_MAP_KEY = ROUTE_ID + "-obsQnAnsMap";
	
	private static final String PROP_DECISION_RULE = "odoo.obs.to.customer.decision.rule.endpoint";
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject(URI_TEST_RULE)
	private MockEndpoint mockTestRuleEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockPatientHandlerEndpoint.reset();
		mockFetchResourceEndpoint.reset();
		mockTestRuleEndpoint.reset();
		AppContext.remove(OBS_QN_ANS_MAP_KEY);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-fetch-resource").skipSendToOriginalEndpoint()
				        .to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-patient-handler").skipSendToOriginalEndpoint()
				        .to(mockPatientHandlerEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldProcessAnObsInsertEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		obsResource.put("person", patientResource);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, PATIENT);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_IF_NOT_EXISTS, true);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		Map<String, Set<Object>> obsQnAnsMap = exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP, Map.class);
		Assert.assertNotNull(obsQnAnsMap);
		Assert.assertEquals(1, obsQnAnsMap.get(CONCEPT_UUID_1).size());
		Assert.assertEquals(2, obsQnAnsMap.get(CONCEPT_UUID_2).size());
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_1).contains(CONCEPT_UUID_A));
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_2).contains(CONCEPT_UUID_B));
		Assert.assertTrue(obsQnAnsMap.get(CONCEPT_UUID_2).contains(CONCEPT_UUID_C));
	}
	
	@Test
	public void shouldProcessAnObsUpdateEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "u");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		obsResource.put("person", patientResource);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, PATIENT);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_IF_NOT_EXISTS, true);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
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
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertNull(exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP));
		assertMessageLogged(Level.DEBUG, "Skipping deleted obs");
	}
	
	@Test
	public void shouldSkipAnEventForAVoidedObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ENTITY, singletonMap("voided", true));
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
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
		obsResource.put("person", singletonMap("uuid", PATIENT_UUID));
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		final String questionConceptUuid = "test-qn-concept";
		final String answerConceptUuid = "test-ans-concept";
		Map cachedObsQnAnsMap = singletonMap(questionConceptUuid, singleton(answerConceptUuid));
		AppContext.add(OBS_QN_ANS_MAP_KEY, cachedObsQnAnsMap);
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		Assert.assertEquals(cachedObsQnAnsMap, exchange.getProperty(EX_PROP_OBS_QN_ANS_MAP, Map.class));
	}
	
	@Test
	public void shouldSkipAnEventForAnObsWithNonMonitoredQuestionConcept() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", "some-question-concept-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
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
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.DEBUG, "Skipping Obs because the answer concept doesn't match any configured answer");
	}
	
	@Test
	public void shouldSkipAnEventForAnObsWithAPatientThatIsNotFound() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("person", singletonMap("uuid", PATIENT_UUID));
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, PATIENT);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "No associated patient found with uuid: " + PATIENT_UUID);
	}
	
	@Test
	public void shouldSkipAnObsThatFailsTheDecisionRule() throws Exception {
		addInlinedPropertiesToEnvironment(env, PROP_DECISION_RULE + "=" + URI_TEST_RULE);
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		obsResource.put("person", patientResource);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(0);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO,
		    "Skipping obs event because it failed the decision rules defined in -> " + URI_TEST_RULE);
	}
	
	@Test
	public void shouldProcessAnObsThatPassesTheDecisionRule() throws Exception {
		addInlinedPropertiesToEnvironment(env, PROP_DECISION_RULE + "=" + URI_TEST_RULE);
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		obsResource.put("person", patientResource);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", singletonMap("uuid", CONCEPT_UUID_A));
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		producerTemplate.send(URI_OBS_HANDLER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
	}
	
}