package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_ADD_EXTRA_QUOTATION_DETAILS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

@TestPropertySource(properties = {"openmrs.baseUrl=www.openmrs.baseUrl", "emr.weight.concept=5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "odoo.weight.field=x_customer_weight_field", "odoo.enable.extra.quotation.details.route=true"})
public class OdooAddExtraQuotationDetailsRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-add-extra-quotation-details";
	
	public static final String PATIENT_RESOURCE = "patient";
	
	public static final String PERSON_RESOURCE = "person";
	
	public static final String PATIENT_DATA = "patientData";
	
	public static final String EX_PROP_CUSTOM_DATA = "customPatientData";
	
	public static final String ID_TYPE_UUID = "8d79403a-c2cc-11de-8d13-0010c6dffd0f";
	
	public static final String ID_TYPE_ID_KEY = "odoo-patient-handler-idTypeId";
	
	@EndpointInject("mock:openmrs-patient-weight-endpoint")
	private MockEndpoint openmrsPatientWeightEndpoint;
	
	@Before
	public void setup() throws Exception {

		AppContext.remove(ID_TYPE_ID_KEY);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				weaveByToString("DynamicTo[{{openmrs.baseUrl}}/ws/rest/v1/obs?concept={{emr.weight.concept}}&patient=${exchangeProperty.patient.get('uuid')}]").replace().toD("mock:openmrs-patient-weight-endpoint");
			}
		});
	}
	
	@After
	public void tearDown() throws Exception {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env, "create.customer.if.not.exist=false");
	}
	
	@Test
	public void shouldAddPatientWeightToOdooCustomerQuotation() throws Exception {
		final Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		exchange.setProperty(PATIENT_RESOURCE, patientResource);
		
		openmrsPatientWeightEndpoint.expectedMessageCount(1);
		openmrsPatientWeightEndpoint.expectedPropertyReceived("patient", patientResource);
		
		openmrsPatientWeightEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody("{\"results\": [{\"uuid\": \"c9067a98-e847-4d2a-a737-aeaf34e1ba15\", \"display\": \"Weight (kg): 55.0\"}, {\"uuid\": \"d0879297-d1b2-4a3a-b700-c0a37bebab35\", \"display\": \"Weight (kg): 95.0\"}, {\"uuid\": \"f9167f7d-8faa-4e47-adbf-b208c0dac6bb\", \"display\": \"Weight (kg): 78.0\"}]}");
		});
		
		Map patientData = new HashMap();
		exchange.setProperty(PATIENT_DATA, patientData);
		
		producerTemplate.send(URI_ADD_EXTRA_QUOTATION_DETAILS, exchange);
		
		openmrsPatientWeightEndpoint.assertIsSatisfied();
		ArrayList requestData = (ArrayList)((ArrayList)exchange.getProperty("requestData"));
		assertEquals("Weight (kg): 55.0", ((Map)requestData.get(1)).get("x_customer_weight_field"));
	}
}
