package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_IS_ENC_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_IS_ENC_VALIDATED;
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

import ch.qos.logback.classic.Level;

public class GetMostRecentEncByFormInVisitRouteTest extends BasePrpRouteTest {
	
	public static final String PARAM_VISIT_UUID = "visitUuid";
	
	public static final String PARAM_FORM_UUID = "formUuid";
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockGetEntityByUuidEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_IS_ENC_VALIDATED)
	private MockEndpoint mockEncValidatedRuleEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetEntityByUuidEndpoint.reset();
		mockEncValidatedRuleEndpoint.reset();
		
		advise(ROUTE_ID_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID).skipSendToOriginalEndpoint().to(mockGetEntityByUuidEndpoint);
				interceptSendToEndpoint(URI_IS_ENC_VALIDATED).skipSendToOriginalEndpoint().to(mockEncValidatedRuleEndpoint);
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
		
		producerTemplate.send(URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT, exchange);
		
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
		
		producerTemplate.send(URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertNull(exchange.getIn().getBody());
	}
	
	@Test
	public void shouldReturnTheObsInTheVisitWithAMatchingTheConceptRecordedOnTheSpecifiedForm() throws Exception {
		final String visitUuid = "visit-uuid";
		final String formUuid = "form-uuid";
		final String encUuid1 = "enc-uuid-1";
		final String encUuid2 = "enc-uuid-2";
		final String encUuid3 = "enc-uuid-3";
		final String encUuid4 = "enc-uuid-4";
		final String encUuid5 = "enc-uuid-5";
		Exchange exchange = new DefaultExchange(camelContext);
		Map params = new HashMap();
		params.put(PARAM_VISIT_UUID, visitUuid);
		params.put(PARAM_FORM_UUID, formUuid);
		exchange.getIn().setBody(params);
		//Encounter recorded on a different form
		Map enc1 = new HashMap();
		enc1.put("uuid", encUuid1);
		enc1.put("encounterDatetime", "2022-04-15T15:00:04.000+0000");
		enc1.put("form", singletonMap("uuid", "form-uuid1"));
		//Encounter recorded on a non validated form
		Map enc2 = new HashMap();
		enc2.put("encounterDatetime", "2022-04-15T15:00:03.000+0000");
		enc2.put("uuid", encUuid2);
		enc2.put("form", singletonMap("uuid", formUuid));
		//Encounter with an earlier encounter date
		Map enc3 = new HashMap();
		enc3.put("encounterDatetime", "2022-04-15T15:00:00.000+0000");
		enc3.put("uuid", encUuid3);
		enc3.put("form", singletonMap("uuid", formUuid));
		Map enc4 = new HashMap();
		enc4.put("encounterDatetime", "2022-04-15T15:00:02.000+0000");
		enc4.put("uuid", encUuid4);
		enc4.put("form", singletonMap("uuid", formUuid));
		Map enc5 = new HashMap();
		enc5.put("encounterDatetime", "2022-04-15T15:00:01.000+0000");
		enc5.put("uuid", encUuid5);
		enc5.put("form", singletonMap("uuid", formUuid));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "visit");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, visitUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		Map visit = singletonMap("encounters", asList(enc1, enc2, enc3, enc4, enc5));
		String visitJson = mapper.writeValueAsString(visit);
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(visitJson));
		
		mockEncValidatedRuleEndpoint.expectedMessageCount(4);
		mockEncValidatedRuleEndpoint.expectedBodiesReceived(encUuid2, encUuid3, encUuid4, encUuid5);
		mockEncValidatedRuleEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(false));
		mockEncValidatedRuleEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(true));
		mockEncValidatedRuleEndpoint.whenExchangeReceived(3, e -> e.getIn().setBody(true));
		mockEncValidatedRuleEndpoint.whenExchangeReceived(4, e -> e.getIn().setBody(true));
		
		producerTemplate.send(URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT, exchange);
		
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
        mockEncValidatedRuleEndpoint.assertIsSatisfied();
		assertEquals(enc4, exchange.getIn().getBody());
		assertMessageLogged(Level.INFO,
		    "Ignoring encounter with uuid " + encUuid2 + " recorded on a form that was not validated");
	}
	
}
