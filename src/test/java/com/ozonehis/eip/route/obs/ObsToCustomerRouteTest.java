package com.ozonehis.eip.route.obs;

import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_CUSTOMER;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_CUSTOMER;
import static com.ozonehis.eip.route.OdooTestConstants.URI_PATIENT_UUID_TO_CUSTOMER;
import static java.util.Collections.singletonMap;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;
import static org.springframework.test.context.support.TestPropertySourceUtils.addInlinedPropertiesToEnvironment;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

import com.ozonehis.eip.route.BaseOdooRouteTest;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/obs/obs-to-customer.xml")
public class ObsToCustomerRouteTest extends BaseOdooRouteTest {
	
	private static final String URI_TEST_RULE = "mock:test-rule";
	
	private static final String TABLE = "obs";
	
	private static final String OBS_UUID = "obs-uuid-1";
	
	private static final String PATIENT_UUID = "patient-uuid";
	
	private static final String PROP_DECISION_RULE = "obs.to.customer.decision.rule.endpoint";
	
	private static final String EX_PROP_SKIP_CUSTOMER_UPDATE = "skipCustomerUpdate";
	
	@EndpointInject("mock:patient-uuid-to-odoo-customer")
	private MockEndpoint mockPatientUuidToCustomerEndpoint;
	
	@EndpointInject(URI_TEST_RULE)
	private MockEndpoint mockTestRuleEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockTestRuleEndpoint.reset();
		mockPatientUuidToCustomerEndpoint.reset();
		
		advise(ROUTE_ID_OBS_TO_CUSTOMER, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_PATIENT_UUID_TO_CUSTOMER).skipSendToOriginalEndpoint()
				        .to(mockPatientUuidToCustomerEndpoint);
			}
			
		});
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
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockPatientUuidToCustomerEndpoint.expectedMessageCount(0);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.expectedBodiesReceived(obsResource);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_OBS_TO_CUSTOMER, exchange);
		
		mockPatientUuidToCustomerEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.expectedBodyReceived();
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
		obsResource.put("person", singletonMap("uuid", PATIENT_UUID));
		exchange.setProperty(EX_PROP_ENTITY, obsResource);
		mockPatientUuidToCustomerEndpoint.expectedMessageCount(1);
		mockPatientUuidToCustomerEndpoint.expectedPropertyReceived(EX_PROP_SKIP_CUSTOMER_UPDATE, true);
		mockPatientUuidToCustomerEndpoint.expectedBodiesReceived(PATIENT_UUID);
		mockTestRuleEndpoint.expectedMessageCount(1);
		mockTestRuleEndpoint.expectedBodiesReceived(obsResource);
		mockTestRuleEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		producerTemplate.send(URI_OBS_TO_CUSTOMER, exchange);
		
		mockPatientUuidToCustomerEndpoint.assertIsSatisfied();
		mockPatientUuidToCustomerEndpoint.expectedBodyReceived();
		mockTestRuleEndpoint.assertIsSatisfied();
		mockTestRuleEndpoint.expectedBodyReceived();
	}
	
}
