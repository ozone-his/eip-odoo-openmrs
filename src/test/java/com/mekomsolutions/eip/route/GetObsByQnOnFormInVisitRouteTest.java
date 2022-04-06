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

public class GetObsByQnOnFormInVisitRouteTest extends BaseOdooRouteTest {
	
	public static final String PARAM_CONCEPT_UUID = "conceptUuid";
	
	public static final String PARAM_VISIT_UUID = "visitUuid";
	
	public static final String PARAM_FORM_UUID = "formUuid";
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockGetEntityByUuidEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetEntityByUuidEndpoint.reset();
		
		advise(ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID).skipSendToOriginalEndpoint().to(mockGetEntityByUuidEndpoint);
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
		Exchange exchange = new DefaultExchange(camelContext);
		Map params = new HashMap();
		params.put(PARAM_VISIT_UUID, visitUuid);
		params.put(PARAM_FORM_UUID, formUuid);
		params.put(PARAM_CONCEPT_UUID, conceptUuid);
		exchange.getIn().setBody(params);
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "visit");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, visitUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		Map expectObs = singletonMap("concept", singletonMap("uuid", conceptUuid));
		Map enc1 = new HashMap();
		enc1.put("form", singletonMap("uuid", "form-uuid1"));
		enc1.put("obs", asList(singletonMap("concept", singletonMap("uuid", conceptUuid))));
		Map enc2 = new HashMap();
		enc2.put("form", singletonMap("uuid", formUuid));
		enc2.put("obs", asList(expectObs));
		Map enc3 = new HashMap();
		enc3.put("form", singletonMap("uuid", "form-uuid2"));
		enc3.put("obs", asList(singletonMap("concept", singletonMap("uuid", conceptUuid))));
		String visitJson = mapper.writeValueAsString(singletonMap("encounters", asList(enc1, enc2, enc3)));
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(visitJson));
		
		producerTemplate.send(URI_GET_OBS_BY_QN_FORM_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertEquals(expectObs, exchange.getIn().getBody());
	}
	
}
