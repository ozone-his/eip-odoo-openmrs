package com.ozonehis.eip.route;

import static com.ozonehis.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.ozonehis.eip.route.OdooTestConstants.URI_ADD_EXTRA_CUSTOMER_DETAILS;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.After;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

@TestPropertySource(
        properties = {
            "openmrs.baseUrl=www.openmrs.baseUrl",
            "odoo.dob.field=x_customer_dob_field",
            "odoo.enable.extra.customer.details.route=true"
        })
public class OdooAddExtraCustomerDetailsRouteTest extends BaseOdooRouteTest {

    private static final String ROUTE_ID = "odoo-add-extra-customer-details";

    public static final String PATIENT_RESOURCE = "patient";

    public static final String PERSON_RESOURCE = "person";

    public static final String PATIENT_DATA = "patientData";

    public static final String EX_PROP_CUSTOM_DATA = "customPatientData";

    public static final String ID_TYPE_UUID = "8d79403a-c2cc-11de-8d13-0010c6dffd0f";

    public static final String ID_TYPE_ID_KEY = "odoo-patient-handler-idTypeId";

    @After
    public void tearDown() throws Exception {
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(env, "create.customer.if.not.exist=false");
    }

    @Test
    public void shouldAddPatientDateOfBirthToOdooCustomer() throws Exception {
        final Exchange exchange = new DefaultExchange(camelContext);
        Map patientResource = new HashMap();
        patientResource.put("uuid", PATIENT_UUID);
        exchange.setProperty(PATIENT_RESOURCE, patientResource);

        Map personResource = new HashMap();
        personResource.put("birthdate", "1971-03-15T00:00:00.000+0000");

        exchange.setProperty(PERSON_RESOURCE, personResource);

        Map patientData = new HashMap();
        exchange.setProperty(PATIENT_DATA, patientData);

        producerTemplate.send(URI_ADD_EXTRA_CUSTOMER_DETAILS, exchange);

        assertEquals("1971-03-15T00:00:00.000+0000", patientData.get("x_customer_dob_field"));
    }
}
