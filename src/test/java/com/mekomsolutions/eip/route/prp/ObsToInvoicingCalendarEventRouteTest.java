package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_CLOSE_EOS_CONCEPT;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_FINAL_ASSMT_CONCEPT;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_FINAL_ASSMT_FORM_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_ASSMT_DECISION;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_CLOSE_EOS;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.FINAL_ASSMT_FORM_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_PARTNERS_BY_USERS;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_IS_ENC_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_OBS_CAPTURED_ON_FORM;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_INVOICE_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_SAVE_CALENDAR_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_PARTNERS_BY_USERS;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_RES_BY_EXT_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_IS_ENC_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_CAPTURED_ON_FORM;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_TO_INVOICE_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_SAVE_CALENDAR_EVENT;
import static java.util.Arrays.asList;
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
import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.ObsCapturedOnFormRuleRouteTest;

@TestPropertySource(properties = APP_PROP_NAME_FINAL_ASSMT_CONCEPT + "=" + CONCEPT_UUID_ASSMT_DECISION)
@TestPropertySource(properties = APP_PROP_NAME_FINAL_ASSMT_FORM_UUID + "=" + FINAL_ASSMT_FORM_UUID)
@TestPropertySource(properties = APP_PROP_NAME_CLOSE_EOS_CONCEPT + "=" + CONCEPT_UUID_CLOSE_EOS)
public class ObsToInvoicingCalendarEventRouteTest extends BasePrpRouteTest {
	
	@EndpointInject("mock:" + ROUTE_ID_OBS_CAPTURED_ON_FORM)
	private MockEndpoint mockObsCapturedOnFormEndpoint;
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockGetEntityByUuidEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_IS_ENC_VALIDATED)
	private MockEndpoint mockEncValidatedEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT)
	private MockEndpoint mockGetMostRecentEncEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO)
	private MockEndpoint mockGetResByExtIdEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_PARTNERS_BY_USERS)
	private MockEndpoint mockGetPartnersByUsersEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_SAVE_CALENDAR_EVENT)
	private MockEndpoint mockSaveCalendarEventEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetEntityByUuidEndpoint.reset();
		mockObsCapturedOnFormEndpoint.reset();
		mockEncValidatedEndpoint.reset();
		mockGetMostRecentEncEndpoint.reset();
		mockGetResByExtIdEndpoint.reset();
		mockGetPartnersByUsersEndpoint.reset();
		mockSaveCalendarEventEndpoint.reset();
		
		advise(ROUTE_ID_OBS_TO_INVOICE_EVENT, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_OBS_CAPTURED_ON_FORM).skipSendToOriginalEndpoint()
				        .to(mockObsCapturedOnFormEndpoint);
				interceptSendToEndpoint(URI_IS_ENC_VALIDATED).skipSendToOriginalEndpoint().to(mockEncValidatedEndpoint);
				interceptSendToEndpoint(URI_GET_MOST_RECENT_ENC_BY_FORM_AND_VISIT).skipSendToOriginalEndpoint()
				        .to(mockGetMostRecentEncEndpoint);
				interceptSendToEndpoint(URI_GET_RES_BY_EXT_ID_FROM_ODOO).skipSendToOriginalEndpoint()
				        .to(mockGetResByExtIdEndpoint);
				interceptSendToEndpoint(URI_GET_PARTNERS_BY_USERS).skipSendToOriginalEndpoint()
				        .to(mockGetPartnersByUsersEndpoint);
				interceptSendToEndpoint(URI_SAVE_CALENDAR_EVENT).skipSendToOriginalEndpoint()
				        .to(mockSaveCalendarEventEndpoint);
				interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID).skipSendToOriginalEndpoint().to(mockGetEntityByUuidEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldSkipTheEventIfTheQuestionForTheObsIsNotFinalDecisionAssessmentConcept() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Map obsRes = singletonMap("concept", singletonMap("uuid", "some-concept-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(0);
		mockGetEntityByUuidEndpoint.expectedMessageCount(0);
		mockEncValidatedEndpoint.expectedMessageCount(0);
		mockGetMostRecentEncEndpoint.expectedMessageCount(0);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipTheEventIfTheValueForTheObsIsNotCloseEndOfEpisodeOfService() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", "some-uuid"));
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(0);
		mockGetEntityByUuidEndpoint.expectedMessageCount(0);
		mockEncValidatedEndpoint.expectedMessageCount(0);
		mockGetMostRecentEncEndpoint.expectedMessageCount(0);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipIfTheObsWasNotRecordedOnTheAssessmentOutComeGsFinalForm() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockGetEntityByUuidEndpoint.expectedMessageCount(0);
		mockEncValidatedEndpoint.expectedMessageCount(0);
		mockGetMostRecentEncEndpoint.expectedMessageCount(0);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldFailIfTheObsEncounterDoesNotExistInOpenmrs() throws Exception {
		final String encUuid = "enc-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		obsRes.put("encounter", singletonMap("uuid", encUuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockEncValidatedEndpoint.expectedMessageCount(0);
		mockGetMostRecentEncEndpoint.expectedMessageCount(0);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertEquals("No encounter found in OpenMRS with uuid: " + encUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldSkipIfTheObsEncounterFormIsNotValidated() throws Exception {
		final String encUuid = "enc-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		obsRes.put("encounter", singletonMap("uuid", encUuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockGetMostRecentEncEndpoint.expectedMessageCount(0);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		Map encRes = singletonMap("uuid", encUuid);
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		
		mockEncValidatedEndpoint.expectedMessageCount(1);
		mockEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipIfNoPhysioSessionEncounterIsFoundInTheVisit() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("visit", singletonMap("uuid", visitUuid));
		obsRes.put("encounter", encRes);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		
		mockEncValidatedEndpoint.expectedMessageCount(1);
		mockEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetMostRecentEncEndpoint.expectedMessageCount(1);
		mockGetMostRecentEncEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipIfNoPhysioServicesWereSelected() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("visit", singletonMap("uuid", visitUuid));
		encRes.put("obs", asList(singletonMap("concept", singletonMap("uuid", "non-pyhsio-service-concept-uuid"))));
		obsRes.put("encounter", encRes);
		Map mostRecentPhysioEncRes = new HashMap();
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockGetResByExtIdEndpoint.expectedMessageCount(0);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		
		mockEncValidatedEndpoint.expectedMessageCount(1);
		mockEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetMostRecentEncEndpoint.expectedMessageCount(1);
		mockGetMostRecentEncEndpoint
		        .whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString(mostRecentPhysioEncRes)));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldFailIfThePatientDoesNotExistInOpenmrs() throws Exception {
		
	}
	
	@Test
	public void shouldFailIfNoBillingGroupIsFoundMatchingTheSpecifiedExternalId() throws Exception {
		
	}
	
	@Test
	public void shouldCreateTheCalendarEventInOdoo() throws Exception {
		
	}
	
	@Test
	public void shouldCreateTheCalendarEventInOdooExcludingPhysioSessionsWithZeroValues() throws Exception {
		
	}
	
}
