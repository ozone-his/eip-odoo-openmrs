package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ENC_VALIDATED_RULE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_NON_VOIDED_OBS_PROCESSOR;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_TO_ORDER_LINE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_CONCEPT_LINE_PROCESSOR;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_UUID_TO_CUSTOMER;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_VOIDED_OBS_PROCESSOR;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "odoo.physio.session.concept.uuids=" + OdooObsToOrderLineRouteTest.CONCEPT_UUID_1 + ","
        + OdooObsToOrderLineRouteTest.CONCEPT_UUID_2)
public class OdooObsToOrderLineRouteTest extends BasePrpRouteTest {
	
	protected static final String ROUTE_ID = "odoo-obs-to-order-line";
	
	protected static final String TABLE = "obs";
	
	public static final String OBS_UUID = "obs-uuid-1";
	
	public static final String CONCEPT_UUID_1 = "concept-uuid-1";
	
	public static final String CONCEPT_UUID_2 = "concept-uuid-2";
	
	public static final String EX_PROP_OBS_QNS = "obsQuestions";
	
	public static final String OBS_QNS_KEY = ROUTE_ID + "-" + EX_PROP_OBS_QNS;
	
	public static final String SET_CACHE_INT_MSG = "Initializing set of physio session count obs questions";
	
	@EndpointInject("mock:voided-obs-to-order-line-processor")
	private MockEndpoint mockVoidedObsProcEndpoint;
	
	@EndpointInject("mock:non-voided-obs-to-order-line-processor")
	private MockEndpoint mockNonVoidedObsProcEndpoint;
	
	@EndpointInject("mock:is-obs-encounter-validated-rule")
	private MockEndpoint mockTestRuleEndpoint;
	
	@EndpointInject("mock:patient-uuid-to-odoo-customer")
	private MockEndpoint mockUuidToCustomerEndpoint;
	
	@EndpointInject("mock:concept-to-order-line-processor")
	private MockEndpoint mockOrderLineProcessorEndpoint;
	
	@Before
	public void setup() throws Exception {
		AppContext.remove(OBS_QNS_KEY);
		mockVoidedObsProcEndpoint.reset();
		mockNonVoidedObsProcEndpoint.reset();
		mockTestRuleEndpoint.reset();
		mockUuidToCustomerEndpoint.reset();
		mockOrderLineProcessorEndpoint.reset();
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_VOIDED_OBS_PROCESSOR).skipSendToOriginalEndpoint().to(mockVoidedObsProcEndpoint);
				interceptSendToEndpoint(URI_NON_VOIDED_OBS_PROCESSOR).skipSendToOriginalEndpoint()
				        .to(mockNonVoidedObsProcEndpoint);
				interceptSendToEndpoint(URI_UUID_TO_CUSTOMER).skipSendToOriginalEndpoint().to(mockUuidToCustomerEndpoint);
				interceptSendToEndpoint(URI_CONCEPT_LINE_PROCESSOR).skipSendToOriginalEndpoint()
				        .to(mockOrderLineProcessorEndpoint);
				interceptSendToEndpoint(URI_ENC_VALIDATED_RULE).skipSendToOriginalEndpoint().to(mockTestRuleEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldCreateAndCacheTheObsQuestionsSetToTheEipContextCache() {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", "some-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		Assert.assertNull(AppContext.get(OBS_QNS_KEY));
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		
		assertMessageLogged(Level.INFO, SET_CACHE_INT_MSG);
		Set questions = (Set) AppContext.get(OBS_QNS_KEY);
		assertTrue(questions.contains(CONCEPT_UUID_1));
		assertTrue(questions.contains(CONCEPT_UUID_2));
	}
	
	@Test
	public void shouldUseTheObsQuestionsSetFromTheEipContextCache() {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", 1);
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		final String qnConceptUuid = "test-qn-concept";
		Set cachedObsQns = singleton(qnConceptUuid);
		AppContext.add(OBS_QNS_KEY, cachedObsQns);
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		
		assertEquals(cachedObsQns, exchange.getProperty(EX_PROP_OBS_QNS, Set.class));
	}
	
	@Test
	public void shouldSkipAnEventForAnObsWithNonMonitoredQuestionConcept() {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", "some-question-concept-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		
		assertMessageLogged(Level.DEBUG, "Skipping non-physio session count obs");
	}
	
	@Test
	public void shouldCallTheCorrectProcessorForANonVoidedObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		final String personUuid = "person-uuid";
		obsResource.put("person", singletonMap("uuid", personUuid));
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", 1);
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockNonVoidedObsProcEndpoint.expectedMessageCount(1);
		mockVoidedObsProcEndpoint.expectedMessageCount(0);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		mockUuidToCustomerEndpoint.expectedMessageCount(1);
		mockUuidToCustomerEndpoint.expectedBodiesReceived(personUuid);
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		mockNonVoidedObsProcEndpoint.assertIsSatisfied();
		mockVoidedObsProcEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
		mockUuidToCustomerEndpoint.assertIsSatisfied();
		mockUuidToCustomerEndpoint.expectedBodyReceived();
	}
	
	@Test
	public void shouldCallTheCorrectProcessorForAVoidedObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("voided", true);
		final String personUuid = "person-uuid";
		obsResource.put("person", singletonMap("uuid", personUuid));
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", 1);
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockNonVoidedObsProcEndpoint.expectedMessageCount(0);
		mockVoidedObsProcEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		mockUuidToCustomerEndpoint.expectedMessageCount(1);
		mockUuidToCustomerEndpoint.expectedBodiesReceived(personUuid);
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		mockNonVoidedObsProcEndpoint.assertIsSatisfied();
		mockVoidedObsProcEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
		mockUuidToCustomerEndpoint.assertIsSatisfied();
		mockUuidToCustomerEndpoint.expectedBodyReceived();
	}
	
	@Test
	public void shouldSkipAnObsThatFailsTheDecisionRule() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", 1);
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockNonVoidedObsProcEndpoint.expectedMessageCount(0);
		mockVoidedObsProcEndpoint.expectedMessageCount(0);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.expectedBodiesReceived(obsResource);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		mockNonVoidedObsProcEndpoint.assertIsSatisfied();
		mockVoidedObsProcEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.expectedBodyReceived();
		assertMessageLogged(Level.INFO, "Skipping obs event because it failed the decision rule");
	}
	
	@Test
	public void shouldProcessAnObsThatPassesTheDecisionRule() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		final String personUuid = "person-uuid";
		obsResource.put("person", singletonMap("uuid", personUuid));
		obsResource.put("concept", singletonMap("uuid", CONCEPT_UUID_1));
		obsResource.put("value", 1);
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockNonVoidedObsProcEndpoint.expectedMessageCount(1);
		mockVoidedObsProcEndpoint.expectedMessageCount(0);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.expectedBodiesReceived(obsResource);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		mockUuidToCustomerEndpoint.expectedMessageCount(1);
		mockUuidToCustomerEndpoint.expectedBodiesReceived(personUuid);
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		mockNonVoidedObsProcEndpoint.assertIsSatisfied();
		mockVoidedObsProcEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.expectedBodyReceived();
		mockUuidToCustomerEndpoint.assertIsSatisfied();
		mockUuidToCustomerEndpoint.expectedBodyReceived();
	}
	
}
