package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.GetResourceByExtIdFromOdooRouteTest.PARAM_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_BASIC_SERVICE_PLAN_FORM_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_GRP_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.APP_PROP_NAME_ID_TYPE_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.BASIC_SERVICE_PLAN_FORM_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_PATIENT_TYPE;
import static com.mekomsolutions.eip.route.OdooTestConstants.CONCEPT_UUID_VALIDATED;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RES_REP;
import static com.mekomsolutions.eip.route.OdooTestConstants.MODEL_NAME_GROUPS;
import static com.mekomsolutions.eip.route.OdooTestConstants.PARAM_MODEL_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_PARTNERS_BY_USERS;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_DISCHARGE_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_SAVE_CALENDAR_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_OBS_BY_QN_FORM_VISIT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_PARTNERS_BY_USERS;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_RES_BY_EXT_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_CAPTURED_ON_FORM;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_OBS_TO_DISCHARGE_EVENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_SAVE_CALENDAR_EVENT;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_ATTENDEE_PARTNER_IDS;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_DESCRIPTION;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_DURATION;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_START;
import static com.mekomsolutions.eip.route.SaveCalendarEventInOdooRouteTest.EX_PROP_SUBJECT;
import static com.mekomsolutions.eip.route.prp.GetPartnerIdsByUserIdsRouteTest.PARAM_USER_IDS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.GetObsByQnOnFormInVisitRouteTest;
import com.mekomsolutions.eip.route.ObsCapturedOnFormRuleRouteTest;

@TestPropertySource(properties = APP_PROP_NAME_GRP_EXT_ID + "=" + ObsToDischargeCalendarEventRouteTest.GROUP_EXT_ID)
@TestPropertySource(properties = APP_PROP_NAME_ID_TYPE_UUID + "=" + ObsToDischargeCalendarEventRouteTest.ID_TYPE_UUID)
@TestPropertySource(properties = APP_PROP_NAME_BASIC_SERVICE_PLAN_FORM_UUID + "=" + BASIC_SERVICE_PLAN_FORM_UUID)
public class ObsToDischargeCalendarEventRouteTest extends BasePrpRouteTest {
	
	protected static final String GROUP_EXT_ID = "Test ext id";
	
	protected static final String GROUP_NAME = "TEST GROUP NAME";
	
	protected static final String ID_TYPE_UUID = "test-id-type-uuid";
	
	private static final String FORM_UUID = "5fa318a9-eade-ea79-a96e-d91754135a5c";
	
	private static final String CONCEPT_UUID_ASSMT_DECISION = "675b4e02-1a96-4eda-af84-b6ebe0d715a4";
	
	private static final String CONCEPT_UUID_CLOSE_EOS = "9bc5c6f2-7c82-11e9-8f9e-2a86e4085a59";
	
	@EndpointInject("mock:obs-captured-on-form-rule")
	private MockEndpoint mockObsCapturedOnFormEndpoint;
	
	@EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
	private MockEndpoint mockGetEntityByUuidEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO)
	private MockEndpoint mockGetResByExtIdEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_PARTNERS_BY_USERS)
	private MockEndpoint mockGetPartnersByUsersEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_SAVE_CALENDAR_EVENT)
	private MockEndpoint mockSaveCalendarEventEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT)
	private MockEndpoint mockGetObsByQnFormVisitEndpoint;
	
	private static boolean loadedExtraRoutes = false;
	
	@Before
	public void setup() throws Exception {
		mockObsCapturedOnFormEndpoint.reset();
		mockGetEntityByUuidEndpoint.reset();
		mockGetResByExtIdEndpoint.reset();
		mockGetPartnersByUsersEndpoint.reset();
		mockSaveCalendarEventEndpoint.reset();
		mockGetObsByQnFormVisitEndpoint.reset();
		
		if (!loadedExtraRoutes) {
			loadXmlRoutesInCamelDirectory(ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC + ".xml");
			loadXmlRoutesInCamelDirectory(ROUTE_ID_GET_OBS_BY_QN_FORM_VISIT + ".xml");
			loadedExtraRoutes = true;
		}
		
		advise(ROUTE_ID_OBS_TO_DISCHARGE_EVENT, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_OBS_CAPTURED_ON_FORM).skipSendToOriginalEndpoint()
				        .to(mockObsCapturedOnFormEndpoint);
				interceptSendToEndpoint(URI_GET_RES_BY_EXT_ID_FROM_ODOO).skipSendToOriginalEndpoint()
				        .to(mockGetResByExtIdEndpoint);
				interceptSendToEndpoint(URI_GET_PARTNERS_BY_USERS).skipSendToOriginalEndpoint()
				        .to(mockGetPartnersByUsersEndpoint);
				interceptSendToEndpoint(URI_SAVE_CALENDAR_EVENT).skipSendToOriginalEndpoint()
				        .to(mockSaveCalendarEventEndpoint);
				interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID).skipSendToOriginalEndpoint().to(mockGetEntityByUuidEndpoint);
				interceptSendToEndpoint(URI_GET_OBS_BY_QN_FORM_VISIT).skipSendToOriginalEndpoint()
				        .to(mockGetObsByQnFormVisitEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldSkipTheEventIfItIsNotAValidatedObs() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipIfTheObsWasNotRecordedOnTheAssessmentOutComeGsFinalForm() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Map obsRes = singletonMap("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		mockGetEntityByUuidEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(false));
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldFailIfTheObsEncounterDoesNotExistInOpenmrs() throws Exception {
		final String encUuid = "enc-uuid";
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		obsRes.put("encounter", singletonMap("uuid", encUuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		mockGetObsByQnFormVisitEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetObsByQnFormVisitEndpoint.assertIsSatisfied();
		assertEquals("No encounter found in OpenMRS with uuid: " + encUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldSkipIfCloseEoSIsNotTheSelectedFinalAssessmentOutcome() throws Exception {
		final String encUuid = "enc-uuid";
		Map assmtDecObsRes = new HashMap();
		assmtDecObsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		assmtDecObsRes.put("value", emptyMap());
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("obs", asList(assmtDecObsRes));
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		obsRes.put("encounter", encRes);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetObsByQnFormVisitEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetObsByQnFormVisitEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldSkipIfThePatientIsNotAdmittedInTheAssociatedVisit() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		Map assmtDecObsRes = new HashMap();
		assmtDecObsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		assmtDecObsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("obs", asList(assmtDecObsRes));
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		obsRes.put("encounter", encRes);
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(1);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "encounter");
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, encUuid);
		mockGetEntityByUuidEndpoint.expectedPropertyReceived(EX_PROP_RES_REP, "full");
		mockGetEntityByUuidEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetObsByQnFormVisitEndpoint.expectedMessageCount(1);
		Map params = new HashMap();
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_CONCEPT_UUID, CONCEPT_UUID_PATIENT_TYPE);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_FORM_UUID, BASIC_SERVICE_PLAN_FORM_UUID);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_VISIT_UUID, visitUuid);
		mockGetObsByQnFormVisitEndpoint.expectedBodiesReceived(params);
		mockGetObsByQnFormVisitEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetObsByQnFormVisitEndpoint.assertIsSatisfied();
		assertNull(getException(exchange));
	}
	
	@Test
	public void shouldFailIfThePatientDoesNotExistInOpenmrs() throws Exception {
		final String visitUuid = "visit-uuid";
		final String patientUuid = "patient-uuid";
		final String encUuid = "enc-uuid";
		Map assmtDecObsRes = new HashMap();
		assmtDecObsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		assmtDecObsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("obs", asList(assmtDecObsRes));
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		obsRes.put("encounter", encRes);
		obsRes.put("person", singletonMap("uuid", patientUuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(2);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, asList(false, false));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME,
		    asList("encounter", "patient"));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID,
		    asList(encUuid, patientUuid));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, asList("full", "full"));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(null));
		
		mockGetObsByQnFormVisitEndpoint.expectedMessageCount(1);
		Map params = new HashMap();
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_CONCEPT_UUID, CONCEPT_UUID_PATIENT_TYPE);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_FORM_UUID, BASIC_SERVICE_PLAN_FORM_UUID);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_VISIT_UUID, visitUuid);
		mockGetObsByQnFormVisitEndpoint.expectedBodiesReceived(params);
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertExchangeReceived(0);
		mockGetEntityByUuidEndpoint.assertExchangeReceived(1);
		mockGetObsByQnFormVisitEndpoint.assertIsSatisfied();
		assertEquals("No patient found in OpenMRS with uuid: " + patientUuid, getErrorMessage(exchange));
	}
	
	@Test
	public void shouldFailIfNoDormitoryGroupIsFoundMatchingTheSpecifiedExternalId() throws Exception {
		final String visitUuid = "visit-uuid";
		final String encUuid = "enc-uuid";
		final String patientUuid = "patient-uuid";
		final String hsuId = "Test Id";
		final String fullName = "Horatio Hornblower";
		Map assmtDecObsRes = new HashMap();
		assmtDecObsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		assmtDecObsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("obs", asList(assmtDecObsRes));
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		obsRes.put("encounter", encRes);
		Map patientRes = new HashMap();
		patientRes.put("uuid", patientUuid);
		patientRes.put("person", singletonMap("display", fullName));
		Map hsuIdRes = new HashMap();
		hsuIdRes.put("identifier", hsuId);
		hsuIdRes.put("identifierType", singletonMap("uuid", ID_TYPE_UUID));
		Map otherIdRes = new HashMap();
		otherIdRes.put("identifier", "some Id");
		otherIdRes.put("identifierType", singletonMap("uuid", "some-id-type-uuid"));
		patientRes.put("identifiers", asList(hsuIdRes, otherIdRes));
		obsRes.put("person", singletonMap("uuid", patientUuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(2);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, asList(false, false));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME,
		    asList("encounter", "patient"));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID,
		    asList(encUuid, patientUuid));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, asList("full", "full"));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(mapper.writeValueAsString(patientRes)));
		
		mockGetObsByQnFormVisitEndpoint.expectedMessageCount(1);
		Map params = new HashMap();
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_CONCEPT_UUID, CONCEPT_UUID_PATIENT_TYPE);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_FORM_UUID, BASIC_SERVICE_PLAN_FORM_UUID);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_VISIT_UUID, visitUuid);
		mockGetObsByQnFormVisitEndpoint.expectedBodiesReceived(params);
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(mapper.writeValueAsString(patientRes)));
		
		mockGetResByExtIdEndpoint.expectedMessageCount(1);
		Map getResByExtParams = new HashMap();
		getResByExtParams.put(PARAM_EXT_ID, GROUP_EXT_ID);
		getResByExtParams.put(PARAM_MODEL_NAME, MODEL_NAME_GROUPS);
		mockGetResByExtIdEndpoint.expectedBodiesReceived(getResByExtParams);
		mockGetResByExtIdEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		
		mockGetPartnersByUsersEndpoint.expectedMessageCount(0);
		mockSaveCalendarEventEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertExchangeReceived(0);
		mockGetEntityByUuidEndpoint.assertExchangeReceived(1);
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		assertEquals("No group found in odoo with external id: " + GROUP_EXT_ID, getErrorMessage(exchange));
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
		Map assmtDecObsRes = new HashMap();
		assmtDecObsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_ASSMT_DECISION));
		assmtDecObsRes.put("value", singletonMap("uuid", CONCEPT_UUID_CLOSE_EOS));
		Map encRes = new HashMap();
		encRes.put("uuid", encUuid);
		encRes.put("obs", asList(assmtDecObsRes));
		encRes.put("visit", singletonMap("uuid", visitUuid));
		Map obsRes = new HashMap();
		obsRes.put("concept", singletonMap("uuid", CONCEPT_UUID_VALIDATED));
		obsRes.put("encounter", encRes);
		Map patientRes = new HashMap();
		patientRes.put("uuid", patientUuid);
		patientRes.put("person", singletonMap("display", fullName));
		Map hsuIdRes = new HashMap();
		hsuIdRes.put("identifier", hsuId);
		hsuIdRes.put("identifierType", singletonMap("uuid", ID_TYPE_UUID));
		Map otherIdRes = new HashMap();
		otherIdRes.put("identifier", "some Id");
		otherIdRes.put("identifierType", singletonMap("uuid", "some-id-type-uuid"));
		patientRes.put("identifiers", asList(hsuIdRes, otherIdRes));
		obsRes.put("person", singletonMap("uuid", patientUuid));
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_ENTITY, obsRes);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS, obsRes);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID, FORM_UUID);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		mockGetEntityByUuidEndpoint.expectedMessageCount(2);
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_IS_SUBRESOURCE, asList(false, false));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_NAME,
		    asList("encounter", "patient"));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RESOURCE_ID,
		    asList(encUuid, patientUuid));
		mockGetEntityByUuidEndpoint.expectedPropertyValuesReceivedInAnyOrder(EX_PROP_RES_REP, asList("full", "full"));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(1, e -> e.getIn().setBody(mapper.writeValueAsString(encRes)));
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(mapper.writeValueAsString(patientRes)));
		
		mockGetObsByQnFormVisitEndpoint.expectedMessageCount(1);
		Map params = new HashMap();
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_CONCEPT_UUID, CONCEPT_UUID_PATIENT_TYPE);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_FORM_UUID, BASIC_SERVICE_PLAN_FORM_UUID);
		params.put(GetObsByQnOnFormInVisitRouteTest.PARAM_VISIT_UUID, visitUuid);
		mockGetObsByQnFormVisitEndpoint.expectedBodiesReceived(params);
		mockGetEntityByUuidEndpoint.whenExchangeReceived(2, e -> e.getIn().setBody(mapper.writeValueAsString(patientRes)));
		
		List<Integer> userIds = asList(userId1, userId2);
		mockGetResByExtIdEndpoint.expectedMessageCount(1);
		Map getResByExtParams = new HashMap();
		getResByExtParams.put(PARAM_EXT_ID, GROUP_EXT_ID);
		getResByExtParams.put(PARAM_MODEL_NAME, MODEL_NAME_GROUPS);
		mockGetResByExtIdEndpoint.expectedBodiesReceived(getResByExtParams);
		mockGetResByExtIdEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(singletonMap("users", userIds)));
		
		mockGetPartnersByUsersEndpoint.expectedMessageCount(1);
		mockGetPartnersByUsersEndpoint.expectedBodiesReceived(singletonMap(PARAM_USER_IDS, userIds));
		mockGetPartnersByUsersEndpoint.whenAnyExchangeReceived(
		    e -> e.getIn().setBody(asList(singletonMap("partner_id", asList(partnerId1, "Partner 1")),
		        singletonMap("partner_id", asList(partnerId2, "Partner 2")))));
		
		mockSaveCalendarEventEndpoint.expectedMessageCount(1);
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_SUBJECT, fullName + "/Discharge");
		final String expectedDescription = "Dear Dormitory Manager, episode of Service for " + fullName + " " + hsuId
		        + " has been closed";
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_DESCRIPTION, expectedDescription);
		LocalDateTime timestamp = LocalDateTime.now(ZoneId.of("UTC"));
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_DURATION, 1440);
		mockSaveCalendarEventEndpoint.expectedPropertyReceived(EX_PROP_ATTENDEE_PARTNER_IDS,
		    new ArrayList(asList(partnerId1, partnerId2)));
		
		producerTemplate.send(URI_OBS_TO_DISCHARGE_EVENT, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertIsSatisfied();
		mockGetEntityByUuidEndpoint.assertExchangeReceived(0);
		mockGetEntityByUuidEndpoint.assertExchangeReceived(1);
		mockGetResByExtIdEndpoint.assertIsSatisfied();
		mockGetPartnersByUsersEndpoint.assertIsSatisfied();
		mockSaveCalendarEventEndpoint.assertIsSatisfied();
		LocalDateTime eventStart = exchange.getProperty(EX_PROP_START, LocalDateTime.class);
		assertTrue(eventStart.isAfter(timestamp));
		assertTrue(eventStart.isBefore(LocalDateTime.now(ZoneId.of("UTC"))));
	}
	
}
