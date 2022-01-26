package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_TO_ORDER_LINE;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/*.xml,classpath*:camel/obs/odoo-obs-to-order-line.xml")
@TestPropertySource(properties = "odoo.physio.session.concept.uuids=" + OdooObsToOrderLineRouteTest.CONCEPT_UUID_1 + ","
        + OdooObsToOrderLineRouteTest.CONCEPT_UUID_2)
public class OdooObsToOrderLineRouteTest extends BasePrpRouteTest {
	
	protected static final String ROUTE_ID = "odoo-obs-to-order-line";
	
	protected static final String TABLE = "obs";
	
	public static final String OBS_UUID = "obs-uuid-1";
	
	public static final String CONCEPT_UUID_1 = "concept-uuid-1";
	
	public static final String CONCEPT_UUID_2 = "concept-uuid-2";
	
	public static final String EX_PROP_OBS_QNS = "obsQuestions";
	
	public static final String OBS_QNS_KEY = ROUTE_ID + "-obsQuestions";
	
	@Before
	public void setup() throws Exception {
		AppContext.remove(OBS_QNS_KEY);
	}
	
	@Test
	public void shouldCreateAndCacheTheObsQuestionsSetToTheEipContextCache() {
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
		
		Assert.assertEquals(cachedObsQns, exchange.getProperty(EX_PROP_OBS_QNS, Set.class));
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
		
		Assert.assertEquals(cachedObsQns, exchange.getProperty(EX_PROP_OBS_QNS, Set.class));
	}
	
	@Test
	public void shouldProcessSkipAnEventForAnObsWithNonMonitoredQuestionConcept() {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent(TABLE, "1", OBS_UUID, "c");
		exchange.setProperty(PROP_EVENT, event);
		Map obsResource = new HashMap();
		obsResource.put("uuid", OBS_UUID);
		obsResource.put("concept", singletonMap("uuid", "some-question-concept-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		
		producerTemplate.send(URI_OBS_TO_ORDER_LINE, exchange);
		
		assertMessageLogged(Level.DEBUG, "Skipping Obs because the question concept doesn't match any configured question");
	}
	
}
