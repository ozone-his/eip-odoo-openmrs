package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_CREATE_IF_NOT_EXISTS;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_PATIENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_MOCK_FETCH_RESOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_UUID_TO_CUSTOMER;

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

public class PatientUuidToOdooCustomerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "patient-uuid-to-odoo-customer";
	
	@EndpointInject(URI_MOCK_FETCH_RESOURCE)
	private MockEndpoint mockFetchResourceEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockPatientHandlerEndpoint.reset();
		mockFetchResourceEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-fetch-resource").skipSendToOriginalEndpoint()
				        .to(mockFetchResourceEndpoint);
				interceptSendToEndpoint("direct:odoo-patient-handler").skipSendToOriginalEndpoint()
				        .to(mockPatientHandlerEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldAddTheCustomerInOdooIfThePatientIsFoundInOpenmrs() throws Exception {
		final String patientUuid = "patient-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(patientUuid);
		Map patientResource = new HashMap();
		patientResource.put("uuid", patientUuid);
		final String patientJson = mapper.writeValueAsString(patientResource);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, patientUuid);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_CREATE_IF_NOT_EXISTS, true);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);
		
		producerTemplate.send(URI_PATIENT_UUID_TO_CUSTOMER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldNotAddTheCustomerInOdooIfThePatientIsNotFoundInOpenmrs() throws Exception {
		final String patientUuid = "patient-uuid";
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(patientUuid);
		mockFetchResourceEndpoint.expectedMessageCount(1);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
		mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, patientUuid);
		mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PATIENT_UUID_TO_CUSTOMER, exchange);
		
		mockFetchResourceEndpoint.assertIsSatisfied();
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.INFO, "No patient found in OpenMRS with uuid: " + patientUuid);
	}
	
}
