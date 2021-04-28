package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_TABLE_REPO_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PERSON_NAME_ADDRESS_HANDLER;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.component.entity.Patient;
import org.openmrs.eip.component.entity.PersonAddress;
import org.openmrs.eip.component.entity.PersonName;
import org.openmrs.eip.component.entity.light.PersonLight;
import org.openmrs.eip.component.repository.PatientRepository;
import org.openmrs.eip.component.repository.PersonAddressRepository;
import org.openmrs.eip.component.repository.PersonNameRepository;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;

public class OdooPersonNameAndAddressHandlerRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-person-name-and-address-handler";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@Autowired
	private PatientRepository patientRepo;
	
	@Autowired
	private PersonNameRepository nameRepo;
	
	@Autowired
	private PersonAddressRepository addressRepo;
	
	@Before
	public void setup() throws Exception {
		mockPatientHandlerEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-patient-handler").skipSendToOriginalEndpoint()
				        .to(mockPatientHandlerEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldLoadThePatientWhenProcessingPersonName() throws Exception {
		Patient expectedPatient = patientRepo.findByUuid(PATIENT_UUID);
		assertNotNull(expectedPatient);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		PersonName personName = nameRepo.findByUuid("0bca417f-fc68-40d7-ae6f-cffca7a5eff1");
		exchange.setProperty(EX_PROP_ENTITY, personName);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, expectedPatient);
		
		producerTemplate.send(URI_PERSON_NAME_ADDRESS_HANDLER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldLoadThePatientWhenProcessingPersonAddress() throws Exception {
		Patient expectedPatient = patientRepo.findByUuid(PATIENT_UUID);
		assertNotNull(expectedPatient);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		PersonAddress personAddress = addressRepo.findByUuid("359022bf-4a58-4732-8cce-1e57f72f47b0");
		exchange.setProperty(EX_PROP_ENTITY, personAddress);
		mockPatientHandlerEndpoint.expectedMessageCount(1);
		mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, expectedPatient);
		
		producerTemplate.send(URI_PERSON_NAME_ADDRESS_HANDLER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfNoPatientIsFound() throws Exception {
		final String badUuid = "bad-uuid";
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_TABLE_REPO_MAP, singletonMap("patient", "patientRepository"));
		PersonLight person = new PersonLight();
		person.setUuid(badUuid);
		PersonAddress personAddress = new PersonAddress();
		personAddress.setPerson(person);
		exchange.setProperty(EX_PROP_ENTITY, personAddress);
		mockPatientHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PERSON_NAME_ADDRESS_HANDLER, exchange);
		
		mockPatientHandlerEndpoint.assertIsSatisfied();
		assertMessageLogged(Level.WARN, "No associated patient found with uuid: " + badUuid);
	}
	
}
