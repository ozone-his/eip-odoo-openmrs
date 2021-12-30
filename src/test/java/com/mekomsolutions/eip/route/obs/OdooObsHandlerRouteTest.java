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

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/*.xml,classpath*:camel/obs/*.xml")
@TestPropertySource(properties = "eip.watchedTables=obs")
@TestPropertySource(properties = "odoo.handler.route=odoo-obs-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=obs:obs")
@TestPropertySource(properties = "odoo.obs.concept.question.answer.mappings=" + OdooObsHandlerRouteTest.CONCEPT_UUID_1 + ":"
        + OdooObsHandlerRouteTest.CONCEPT_UUID_A + "," + OdooObsHandlerRouteTest.CONCEPT_UUID_2 + ":"
        + OdooObsHandlerRouteTest.CONCEPT_UUID_B + "^" + OdooObsHandlerRouteTest.CONCEPT_UUID_C)
public class OdooObsHandlerRouteTest extends BaseOdooRouteTest {
	
	protected static final String ROUTE_ID = "odoo-obs-handler";
	
	protected static final String TABLE = "obs";
	
	protected static final String PATIENT = "patient";
	
	public static final String OBS_UUID = "obs-uuid-1";
	
	public static final String PATIENT_UUID = "patient-uuid";
	
	public static final String CONCEPT_UUID_1 = "concept-uuid-1";
	
	public static final String CONCEPT_UUID_2 = "concept-uuid-2";
	
	public static final String CONCEPT_UUID_A = "concept-uuid-1";
	
	public static final String CONCEPT_UUID_B = "concept-uuid-b";
	
	public static final String CONCEPT_UUID_C = "concept-uuid-c";
	
	public static final String EX_PROP_OBS_QN_ANS_MAP = "obsQnAnsMap";
	
	public static final String EX_PROP_CREATE_IF_NOT_EXISTS = "createCustomer";
	
	public static final String EX_PROP_PATIENT = PATIENT;
	
	public static final String OBS_QN_ANS_MAP_KEY = ROUTE_ID + "-obsQnAnsMap";
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockPatientHandlerEndpoint.reset();
		mockFetchResourceEndpoint.reset();
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
	public void shouldProcessSkipAnObsDeleteEvent() throws Exception {
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
	public void shouldProcessSkipAnEventForAVoidedObs() throws Exception {
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
	public void shouldProcessSkipAnEventForAnObsWithNonMonitoredQuestionConcept() throws Exception {
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
	public void shouldProcessSkipAnEventForAnObsWithNonMonitoredAnswerConcept() throws Exception {
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
	
}
