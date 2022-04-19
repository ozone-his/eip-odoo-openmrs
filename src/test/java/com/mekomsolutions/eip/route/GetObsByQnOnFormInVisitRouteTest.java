package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_OBS_BY_QN_FORM_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

import com.mekomsolutions.eip.route.prp.BasePrpRouteTest;

import ch.qos.logback.classic.Level;

public class GetObsByQnOnFormInVisitRouteTest extends BasePrpRouteTest {
	
	public static final String PARAM_CONCEPT_UUID = "conceptUuid";
	
	public static final String PARAM_VISIT_UUID = "visitUuid";
	
	public static final String PARAM_FORM_UUID = "formUuid";
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockGetEntityByUuidEndpoint;
	
	@EndpointInject("mock:is-obs-form-validated-rule")
	private MockEndpoint mockFormValidatedRuleEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetEntityByUuidEndpoint.reset();
		mockFormValidatedRuleEndpoint.reset();
		
		advise(ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID).skipSendToOriginalEndpoint().to(mockGetEntityByUuidEndpoint);
				interceptSendToEndpoint("direct:is-obs-form-validated-rule").skipSendToOriginalEndpoint()
				        .to(mockFormValidatedRuleEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldFailIfTheVisitDoesNotExistInOpenmrs() throws Exception {
		final String visitUuid = "visit-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(singletonMap(PARAM_VISIT_UUID, visitUuid));
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "visit");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, visitUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_GET_OBS_BY_QN_FORM_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertEquals("No visit found in OpenMRS with uuid: " + visitUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldReturnNullIfTheVisitHasNoEncounters() throws Exception {
		final String visitUuid = "visit-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map params = new HashMap();
		params.put(PARAM_VISIT_UUID, visitUuid);
		params.put(PARAM_FORM_UUID, "some-form-uuid");
		exchange.getIn().setBody(params);
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "visit");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, visitUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		String visitJson = mapper.writeValueAsString(singletonMap("encounters", emptyList()));
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(visitJson));
		
		producerTemplate.send(URI_GET_OBS_BY_QN_FORM_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertNull(exchange.getIn().getBody());
	}
	
	@Test
	public void shouldReturnNullIfTheVisitHasNoEncounterCapturedOnTheSpecifiedForm() throws Exception {
		final String visitUuid = "visit-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		Map params = new HashMap();
		params.put(PARAM_VISIT_UUID, visitUuid);
		params.put(PARAM_FORM_UUID, "form-uuid");
		exchange.getIn().setBody(params);
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "visit");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, visitUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		String visitJson = mapper.writeValueAsString(
		    singletonMap("encounters", asList(singletonMap("form", singletonMap("uuid", "some-form-uuid")))));
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(visitJson));
		
		producerTemplate.send(URI_GET_OBS_BY_QN_FORM_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertNull(exchange.getIn().getBody());
	}
	
	@Test
	public void shouldReturnTheObsInTheVisitWithAMatchingTheConceptRecordedOnTheSpecifiedForm() throws Exception {
		final String visitUuid = "visit-uuid";
		final String formUuid = "form-uuid";
		final String conceptUuid = "concept-uuid";
		final String encUuid2 = "enc-uuid-2";
		final String encUuid3 = "enc-uuid-3";
		Exchange exchange = new DefaultExchange(camelContext);
		Map params = new HashMap();
		params.put(PARAM_VISIT_UUID, visitUuid);
		params.put(PARAM_FORM_UUID, formUuid);
		params.put(PARAM_CONCEPT_UUID, conceptUuid);
		exchange.getIn().setBody(params);
		mockGetEntityByUuidEndpoint.expectedMessageCount(3);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, false, false, false);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME, "visit", "encounter",
		    "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID, visitUuid, encUuid2,
		    encUuid3);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, "full", "full", "full");
		Map expectedObsConcept = singletonMap("uuid", conceptUuid);
		final String obsUuid1 = "obs-1";
		Map obsForEnc2 = new HashMap();
		obsForEnc2.put("uuid", obsUuid1);
		obsForEnc2.put("concept", expectedObsConcept);
		Map obsForEnc3 = new HashMap();
		obsForEnc3.put("uuid", "obs-3");
		obsForEnc3.put("concept", expectedObsConcept);
		Map enc1 = new HashMap();
		enc1.put("form", singletonMap("uuid", "form-uuid1"));
		Map enc2 = new HashMap();
		enc2.put("uuid", encUuid2);
		enc2.put("form", singletonMap("uuid", formUuid));
		Map enc3 = new HashMap();
		enc3.put("uuid", encUuid3);
		enc3.put("form", singletonMap("uuid", formUuid));
		String visitJson = mapper.writeValueAsString(singletonMap("encounters", asList(enc1, enc2, enc3)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(visitJson));
		String enc2Json = mapper.writeValueAsString(singletonMap("obs", asList(obsForEnc2)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(enc2Json));
		String enc3Json = mapper.writeValueAsString(singletonMap("obs", asList(obsForEnc3)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(3, e -> e.getIn().setBody(enc3Json));
		
		mockFormValidatedRuleEndpoint.expectedMessageCount(2);
		mockFormValidatedRuleEndpoint.expectedBodiesReceived(obsForEnc2, obsForEnc3);
		mockFormValidatedRuleEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(false));
		mockFormValidatedRuleEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(true));
		
		producerTemplate.send(URI_GET_OBS_BY_QN_FORM_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertEquals(obsForEnc3, exchange.getIn().getBody());
		assertMessageLogged(Level.INFO, "Ignoring obs with uuid " + obsUuid1 + " recorded on a form that was not validated");
	}
	
}
