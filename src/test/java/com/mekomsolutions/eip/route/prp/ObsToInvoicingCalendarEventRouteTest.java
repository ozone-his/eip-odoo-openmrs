package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.GetResourceByExtIdFromOdooRouteTest.PARAM_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_CLOSE_EOS_CONCEPT;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_FINAL_ASSMT_CONCEPT;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_FINAL_ASSMT_FORM_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_ID_TYPE_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_INVOICE_GRP_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_ASSMT_DECISION;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_CLOSE_EOS;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_PHYSIO_GRP_OTHER;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_PHYSIO_GRP_POST_AT;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_PHYSIO_GRP_PRE_AT;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_PHYSIO_IND_AT;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_PHYSIO_IND_NON_AT;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE_GRP_OTHER;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE_GRP_POST;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE_GRP_PRE;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE_IND_NON_AT;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE_IND_POST;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_SERVICE_IND_PRE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.FINAL_ASSMT_FORM_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.MODEL_NAME_GROUPS;
import static com.mekomsolutions.eip.route.OdooTestConstants.PARAM_MODEL_NAME;
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
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_ATTENDEE_PARTNER_IDS;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_DESCRIPTION;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_DURATION;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_START;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_SUBJECT;
import static com.mekomsolutions.eip.route.prp.GetPartnerIdsByUserIdsRouteTest.PARAM_USER_IDS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.ObsCapturedOnFormRuleRouteTest;

@TestPropertySource(properties = APP_PROP_NAME_FINAL_ASSMT_CONCEPT + "=" + CONCEPT_UUID_ASSMT_DECISION)
@TestPropertySource(properties = APP_PROP_NAME_FINAL_ASSMT_FORM_UUID + "=" + FINAL_ASSMT_FORM_UUID)
@TestPropertySource(properties = APP_PROP_NAME_CLOSE_EOS_CONCEPT + "=" + CONCEPT_UUID_CLOSE_EOS)
@TestPropertySource(properties = APP_PROP_NAME_INVOICE_GRP_EXT_ID + "=" + ObsToInvoicingCalendarEventRouteTest.GRP_EXT_ID)
@TestPropertySource(properties = APP_PROP_NAME_ID_TYPE_UUID + "=" + ObsToDischargeCalendarEventRouteTest.ID_TYPE_UUID)
public class ObsToInvoicingCalendarEventRouteTest extends BasePrpRouteTest {
	
	protected static final String GRP_EXT_ID = "Test final assmt group ext id";
	
	protected static final String ID_TYPE_UUID = "test-id-type-uuid";
	
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
	
	public static final String EX_PROP_SERVICE_COUNTS = ROUTE_ID_OBS_TO_INVOICE_EVENT + "-physioServiceCount";
	
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
		mockGetMostRecentEncEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString("{}")));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertEquals(0, exchange.getProperty(EX_PROP_SERVICE_COUNTS, List.class).size());
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipSelectedPhysioServiceThatHaveZeroSessions() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map serviceObs = new HashMap();
		serviceObs.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		serviceObs.put("value", singletonMap("uuid", CONCEPT_UUID_SERVICE_IND_PRE));
		encRes.put("obs", asList(serviceObs));
		obsRes.put("encounter", encRes);
		Map mostRecentPhysioEncRes = new HashMap();
		Map indPreSessionObs = new HashMap();
		indPreSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_IND_AT));
		indPreSessionObs.put("value", 0);
		mostRecentPhysioEncRes.put("obs", asList(indPreSessionObs));
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
		mockGetMostRecentEncEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mostRecentPhysioEncRes));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertEquals(0, exchange.getProperty(EX_PROP_SERVICE_COUNTS, List.class).size());
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldFailIfThePatientDoesNotExistInOpenmrs() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		final String patientUuid = "patient-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		obsRes.put("person", singletonMap("uuid", patientUuid));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map serviceObs = new HashMap();
		serviceObs.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		serviceObs.put("value", singletonMap("uuid", CONCEPT_UUID_SERVICE_IND_PRE));
		encRes.put("obs", asList(serviceObs));
		obsRes.put("encounter", encRes);
		Map mostRecentPhysioEncRes = new HashMap();
		Map indPreSessionObs = new HashMap();
		indPreSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_IND_AT));
		indPreSessionObs.put("value", 1);
		mostRecentPhysioEncRes.put("obs", asList(indPreSessionObs));
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
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(2);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, false, false);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME, "encounter", "patient");
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID, encUuid, patientUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, "full", "full");
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(null));
		
		mockEncValidatedEndpoint.expectedMessageCount(1);
		mockEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetMostRecentEncEndpoint.expectedMessageCount(1);
		mockGetMostRecentEncEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mostRecentPhysioEncRes));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertExchangeReceived(0);
		mockGetEntityByUuidEndpoint.assertExchangeReceived(1);
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertEquals("No patient found in OpenMRS with uuid: " + patientUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfNoBillingGroupIsFoundMatchingTheSpecifiedExternalId() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		final String patientUuid = "patient-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		obsRes.put("person", singletonMap("uuid", patientUuid));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map serviceObs = new HashMap();
		serviceObs.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		serviceObs.put("value", singletonMap("uuid", CONCEPT_UUID_SERVICE_IND_PRE));
		encRes.put("obs", asList(serviceObs));
		obsRes.put("encounter", encRes);
		Map mostRecentPhysioEncRes = new HashMap();
		Map indPreSessionObs = new HashMap();
		indPreSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_IND_AT));
		indPreSessionObs.put("value", 1);
		mostRecentPhysioEncRes.put("obs", asList(indPreSessionObs));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(2);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, false, false);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME, "encounter", "patient");
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID, encUuid, patientUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, "full", "full");
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody("{}"));
		
		mockEncValidatedEndpoint.expectedMessageCount(1);
		mockEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetMostRecentEncEndpoint.expectedMessageCount(1);
		mockGetMostRecentEncEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mostRecentPhysioEncRes));
		
		mockGetResByExtIdEndpoint.expectedMessageCount(1);
		Map getResByExtParams = new HashMap();
		getResByExtParams.put(PARAM_EXT_ID, GRP_EXT_ID);
		getResByExtParams.put(PARAM_MODEL_NAME, MODEL_NAME_GROUPS);
		mockGetResByExtIdEndpoint.expectedBodiesReceived(getResByExtParams);
		mockGetResByExtIdEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertExchangeReceived(0);
		mockGetEntityByUuidEndpoint.assertExchangeReceived(1);
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertEquals("No group found in odoo with external id: " + GRP_EXT_ID, getErrorMessage(exchange));
	}
	
	private Map createAnswerConcept(String uuid, String display) {
		Map concept = new HashMap();
		concept.put("uuid", uuid);
		concept.put("display", display);
		return concept;
	}
	
	@Test
	public void shouldCreateTheCalendarEventInOdoo() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		final String patientUuid = "patient-uuid";
		final String hsuId = "Test Id";
		final String fullName = "Horatio Hornblower";
		final Integer userId1 = 11;
		final Integer userId2 = 17;
		final Integer partnerId1 = 31;
		final Integer partnerId2 = 37;
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		obsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		obsRes.put("person", singletonMap("uuid", patientUuid));
		Map patientRes = new HashMap();
		patientRes.put("uuid", patientUuid);
		patientRes.put("person", singletonMap("display", fullName));
		Map hsuIdRes = new HashMap();
		hsuIdRes.put("identifier", hsuId);
		hsuIdRes.put("identifierType", singletonMap("uuid", ID_TYPE_UUID));
		patientRes.put("identifiers", asList(hsuIdRes));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map indPreConcept = new HashMap();
		final String indPre = "Individual Pres Fitting";
		final String indPost = "Individual Post Fitting ";
		final String grpPre = "Group Pre Fitting";
		final String grpPost = "Group Post Fitting";
		final String indNonAt = "Individual Non AT Related";
		final String grpOther = "Group Other";
		indPreConcept.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		indPreConcept.put("value", createAnswerConcept(CONCEPT_UUID_SERVICE_IND_PRE, indPre));
		Map indPostConcept = new HashMap();
		indPostConcept.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		indPostConcept.put("value", createAnswerConcept(CONCEPT_UUID_SERVICE_IND_POST, indPost));
		Map grpPreConcept = new HashMap();
		grpPreConcept.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		grpPreConcept.put("value", createAnswerConcept(CONCEPT_UUID_SERVICE_GRP_PRE, grpPre));
		Map grpPostConcept = new HashMap();
		grpPostConcept.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		grpPostConcept.put("value", createAnswerConcept(CONCEPT_UUID_SERVICE_GRP_POST, grpPost));
		Map indNonAtConcept = new HashMap();
		indNonAtConcept.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		indNonAtConcept.put("value", createAnswerConcept(CONCEPT_UUID_SERVICE_IND_NON_AT, indNonAt));
		Map grpOtherConcept = new HashMap();
		grpOtherConcept.put("concept", singletonMap("uuid", CONCEPT_UUID_SERVICE));
		grpOtherConcept.put("value", createAnswerConcept(CONCEPT_UUID_SERVICE_GRP_OTHER, grpOther));
		encRes.put("obs",
		    asList(indPreConcept, indPostConcept, grpPreConcept, grpPostConcept, indNonAtConcept, grpOtherConcept));
		obsRes.put("encounter", encRes);
		Map mostRecentPhysioEncRes = new HashMap();
		Map indAtRelatedSessionObs = new HashMap();
		final int countIndAt = 1;
		final int countGrpPreAt = 2;
		final int countGrpPostAt = 3;
		final int countIndNonAt = 4;
		final int countGrpOther = 5;
		indAtRelatedSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_IND_AT));
		indAtRelatedSessionObs.put("value", countIndAt);
		Map grpPreSessionObs = new HashMap();
		grpPreSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_GRP_PRE_AT));
		grpPreSessionObs.put("value", countGrpPreAt);
		Map grpPostSessionObs = new HashMap();
		grpPostSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_GRP_POST_AT));
		grpPostSessionObs.put("value", countGrpPostAt);
		Map indNonAtRelatedSessionObs = new HashMap();
		indNonAtRelatedSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_IND_NON_AT));
		indNonAtRelatedSessionObs.put("value", countIndNonAt);
		Map grpOtherSessionObs = new HashMap();
		grpOtherSessionObs.put("concept", singletonMap("uuid", CONCEPT_UUID_PHYSIO_GRP_OTHER));
		grpOtherSessionObs.put("value", countGrpOther);
		mostRecentPhysioEncRes.put("obs", asList(indAtRelatedSessionObs, grpPreSessionObs, grpPostSessionObs,
		    indNonAtRelatedSessionObs, grpOtherSessionObs));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID,
		    FINAL_ASSMT_FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(2);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, false, false);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME, "encounter", "patient");
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID, encUuid, patientUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, "full", "full");
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(mapper.writeValueAsString(patientRes)));
		
		mockEncValidatedEndpoint.expectedMessageCount(1);
		mockEncValidatedEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetMostRecentEncEndpoint.expectedMessageCount(1);
		mockGetMostRecentEncEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mostRecentPhysioEncRes));
		
		mockGetResByExtIdEndpoint.expectedMessageCount(1);
		List<Integer> userIds = asList(userId1, userId2);
		Map getResByExtParams = new HashMap();
		getResByExtParams.put(PARAM_EXT_ID, GRP_EXT_ID);
		getResByExtParams.put(PARAM_MODEL_NAME, MODEL_NAME_GROUPS);
		mockGetResByExtIdEndpoint.expectedBodiesReceived(getResByExtParams);
		mockGetResByExtIdEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(singletonMap("users", userIds)));
		
		mockGetPartnersByUsersEndpoint.expectedMessageCount(1);
		mockGetPartnersByUsersEndpoint.expectedBodiesReceived(singletonMap(PARAM_USER_IDS, userIds));
		mockGetPartnersByUsersEndpoint.whenAnyExchangeReceived(
		    e -> e.getIn().setBody(asList(singletonMap("partner_id", asList(partnerId1, "Partner 1")),
		        singletonMap("partner_id", asList(partnerId2, "Partner 2")))));
		
		mockSaveCalendarEventEndpoint.expectedMessageCount(1);
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_SUBJECT, fullName + "/Final Assessment");
		final String expectedDescr = "Physiotherapy sessions for " + fullName + " " + hsuId + " - "
		        + StringUtils.join(asList(indPre + "(" + countIndAt + ")", grpPre + "(" + countGrpPreAt + ")",
		            grpPost + "(" + countGrpPostAt + ")", indNonAt + "(" + countIndNonAt + ")",
		            grpOther + "(" + countGrpOther + ")"), " ");
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_DESCRIPTION, expectedDescr);
		LocalDateTime timestamp = LocalDateTime.now(ZoneId.of("UTC"));
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_DURATION, 1440);
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_ATTENDEE_PARTNER_IDS,
		    new ArrayList(asList(partnerId1, partnerId2)));
		
		producerTemplate.send(URI_OBS_TO_INVOICE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertExchangeReceived(0);
		mockGetEntityByUuidEndpoint.assertExchangeReceived(1);
		mockEncValidatedEndpoint.assertIsSatisfied();
		mockGetMostRecentEncEndpoint.assertIsSatisfied();
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		LocalDateTime eventStart = exchange.getProperty(EX_PROP_START, LocalDateTime.class);
		assertTrue(eventStart.isAfter(timestamp));
		assertTrue(eventStart.isBefore(LocalDateTime.now(ZoneId.of("UTC"))));
	}
	
}
