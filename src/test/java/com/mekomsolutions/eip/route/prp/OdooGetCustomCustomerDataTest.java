package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_PATIENT;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_CUSTOM_DATA;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Test;

public class OdooGetCustomCustomerDataTest extends BasePrpRouteTest {
	
	protected static final String ROUTE_ID = "odoo-callback-get-custom-customer-data";
	
	private static final String EX_PROP_CUSTOM_DATA = "customPatientData";
	
	private static final String FIELD_GENDER = "gender";
	
	private static final String ODOO_GENDER_M = "m";
	
	private static final String ODOO_GENDER_F = "f";
	
	@Test
	public void shouldSetOdooGenderMatchingTheOpenmrsMaleGender() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", "patient-uuid");
		patientResource.put("person", Collections.singletonMap(FIELD_GENDER, "M"));
		Map customData = new HashMap();
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CUSTOM_DATA, customData);
		
		producerTemplate.send(URI_CUSTOM_DATA, exchange);
		
		Assert.assertEquals(ODOO_GENDER_M, customData.get(FIELD_GENDER));
	}
	
	@Test
	public void shouldSetOdooGenderMatchingTheOpenmrsMaleGenderIgnoringCase() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", "patient-uuid");
		patientResource.put("person", Collections.singletonMap(FIELD_GENDER, "m"));
		Map customData = new HashMap();
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CUSTOM_DATA, customData);
		
		producerTemplate.send(URI_CUSTOM_DATA, exchange);
		
		Assert.assertEquals(ODOO_GENDER_M, customData.get(FIELD_GENDER));
	}
	
	@Test
	public void shouldSetOdooGenderMatchingTheOpenmrsFemaleGender() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", "patient-uuid");
		patientResource.put("person", Collections.singletonMap(FIELD_GENDER, "F"));
		Map customData = new HashMap();
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CUSTOM_DATA, customData);
		
		producerTemplate.send(URI_CUSTOM_DATA, exchange);
		
		Assert.assertEquals(ODOO_GENDER_F, customData.get(FIELD_GENDER));
	}
	
	@Test
	public void shouldSetOdooGenderMatchingTheOpenmrsFemaleGenderIgnoringCase() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", "patient-uuid");
		patientResource.put("person", Collections.singletonMap(FIELD_GENDER, "f"));
		Map customData = new HashMap();
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CUSTOM_DATA, customData);
		
		producerTemplate.send(URI_CUSTOM_DATA, exchange);
		
		Assert.assertEquals(ODOO_GENDER_F, customData.get(FIELD_GENDER));
	}
	
	@Test
	public void shouldNotSetOdooGenderIfOpenmrsGenderIsNull() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", "patient-uuid");
		patientResource.put("person", Collections.emptyMap());
		Map customData = new HashMap();
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CUSTOM_DATA, customData);
		
		producerTemplate.send(URI_CUSTOM_DATA, exchange);
		
		Assert.assertNull(customData.get(FIELD_GENDER));
	}
	
	@Test
	public void shouldNotSetOdooGenderIfOpenmrsGenderIsNeitherMaleNorFemale() {
		Exchange exchange = new DefaultExchange(camelContext);
		Map patientResource = new HashMap();
		patientResource.put("uuid", "patient-uuid");
		patientResource.put("person", Collections.singletonMap(FIELD_GENDER, "O"));
		Map customData = new HashMap();
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CUSTOM_DATA, customData);
		
		producerTemplate.send(URI_CUSTOM_DATA, exchange);
		
		Assert.assertNull(customData.get(FIELD_GENDER));
	}
	
}
