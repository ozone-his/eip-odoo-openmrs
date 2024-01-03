package com.ozonehis.eip.odooopenmrs.route;

import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.EX_PROP_CREATE_IF_NOT_EXISTS;
import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.EX_PROP_PATIENT;
import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static com.ozonehis.eip.odooopenmrs.route.OdooTestConstants.URI_PATIENT_UUID_TO_CUSTOMER;

import ch.qos.logback.classic.Level;
import java.util.HashMap;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PatientUuidToOdooCustomerRouteTest extends BaseOdooRouteTest {

    private static final String ROUTE_ID = "patient-uuid-to-odoo-customer";

    @EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
    private MockEndpoint mockFetchResourceEndpoint;

    @EndpointInject("mock:odoo-patient-handler")
    private MockEndpoint mockPatientHandlerEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInCamelDirectory("patient-uuid-to-odoo-customer.xml");

        mockPatientHandlerEndpoint.reset();
        mockFetchResourceEndpoint.reset();

        advise(ROUTE_ID, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint("direct:get-entity-by-uuid-from-openmrs")
                        .skipSendToOriginalEndpoint()
                        .to(mockFetchResourceEndpoint);
                interceptSendToEndpoint("direct:odoo-patient-handler")
                        .skipSendToOriginalEndpoint()
                        .to(mockPatientHandlerEndpoint);
            }
        });
    }

    @Test
    public void shouldAddTheCustomerInOdooIfThePatientIsFoundInOpenmrs() throws Exception {
        final String patientUuid = "patient-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(patientUuid);
        var patientResource = new HashMap<>();
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
