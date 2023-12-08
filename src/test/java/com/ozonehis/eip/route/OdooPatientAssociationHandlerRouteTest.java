package com.ozonehis.eip.route;

import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_ENTITY;
import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_IS_SUBRESOURCE;
import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_RESOURCE_ID;
import static com.ozonehis.eip.route.OdooTestConstants.EX_PROP_RESOURCE_NAME;
import static com.ozonehis.eip.route.OdooTestConstants.PATIENT_ID_UUID;
import static com.ozonehis.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.ozonehis.eip.route.OdooTestConstants.URI_GET_ENTITY_BY_UUID;
import static com.ozonehis.eip.route.OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID;
import static com.ozonehis.eip.route.OdooTestConstants.URI_PATIENT_ASSOCIATION_HANDLER;
import static java.util.Collections.singletonMap;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import ch.qos.logback.classic.Level;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;

public class OdooPatientAssociationHandlerRouteTest extends BaseOdooRouteTest {

    private static final String ROUTE_ID = "odoo-patient-association-handler";

    public static final String EX_PROP_PATIENT = "patient";

    @EndpointInject("mock:odoo-patient-handler")
    private MockEndpoint mockPatientHandlerEndpoint;

    @EndpointInject(URI_MOCK_GET_ENTITY_BY_UUID)
    private MockEndpoint mockFetchResourceEndpoint;

    @Before
    public void setup() throws Exception {
        mockFetchResourceEndpoint.reset();
        mockPatientHandlerEndpoint.reset();
        advise(ROUTE_ID, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint(URI_GET_ENTITY_BY_UUID)
                        .skipSendToOriginalEndpoint()
                        .to(mockFetchResourceEndpoint);
                interceptSendToEndpoint("direct:odoo-patient-handler")
                        .skipSendToOriginalEndpoint()
                        .to(mockPatientHandlerEndpoint);
            }
        });
    }

    @Test
    public void shouldLoadThePatientWhenProcessingPersonName() throws Exception {
        final String personUuid = "ba3b12d1-5c4f-415f-871b-b98a22137604";
        Map personResource = singletonMap("uuid", personUuid);
        Map nameResource = new HashMap();
        final String nameUuid = "0bca417f-fc68-40d7-ae6f-cffca7a5eff1";
        nameResource.put("uuid", nameUuid);
        nameResource.put("person", personResource);
        Map patientResource = new HashMap();
        patientResource.put("uuid", personUuid);
        Event event = createEvent("person_name", "1", nameUuid, "c");
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        exchange.setProperty(EX_PROP_ENTITY, nameResource);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, personUuid);
        final String patientJson = mapper.writeValueAsString(patientResource);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
        mockPatientHandlerEndpoint.expectedMessageCount(1);
        mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);

        producerTemplate.send(URI_PATIENT_ASSOCIATION_HANDLER, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        mockPatientHandlerEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldLoadThePatientWhenProcessingPersonAddress() throws Exception {
        final String personUuid = "ba3b12d1-5c4f-415f-871b-b98a22137604";
        Map personResource = singletonMap("uuid", personUuid);
        Map addressResource = new HashMap();
        final String addressUuid = "359022bf-4a58-4732-8cce-1e57f72f47b0";
        addressResource.put("uuid", addressUuid);
        addressResource.put("person", personResource);
        Map patientResource = new HashMap();
        patientResource.put("uuid", personUuid);
        Event event = createEvent("person_address", "1", addressUuid, "c");
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        exchange.setProperty(EX_PROP_ENTITY, addressResource);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, personUuid);
        final String patientJson = mapper.writeValueAsString(patientResource);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
        mockPatientHandlerEndpoint.expectedMessageCount(1);
        mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);

        producerTemplate.send(URI_PATIENT_ASSOCIATION_HANDLER, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        mockPatientHandlerEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldLoadThePatientWhenProcessingPatientIdentifier() throws Exception {
        Map patientResource = new HashMap();
        patientResource.put("uuid", PATIENT_UUID);
        Map idResource = new HashMap();
        idResource.put("uuid", PATIENT_ID_UUID);
        idResource.put("patient", patientResource);
        Event event = createEvent("patient_identifier", "1", PATIENT_ID_UUID, "c");
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        exchange.setProperty(EX_PROP_ENTITY, idResource);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, PATIENT_UUID);
        final String patientJson = mapper.writeValueAsString(patientResource);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientJson));
        mockPatientHandlerEndpoint.expectedMessageCount(1);
        mockPatientHandlerEndpoint.expectedPropertyReceived(EX_PROP_PATIENT, patientResource);

        producerTemplate.send(URI_PATIENT_ASSOCIATION_HANDLER, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        mockPatientHandlerEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldFailIfNoPatientIsFound() throws Exception {
        final String personUuid = "ba3b12d1-5c4f-415f-871b-b98a22137604";
        Map personResource = singletonMap("uuid", personUuid);
        Map addressResource = new HashMap();
        final String addressUuid = "359022bf-4a58-4732-8cce-1e57f72f47b0";
        addressResource.put("uuid", addressUuid);
        addressResource.put("person", personResource);
        Event event = createEvent("person_address", "1", addressUuid, "c");
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        addressResource.put("person", personResource);
        exchange.setProperty(EX_PROP_ENTITY, addressResource);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_NAME, "patient");
        mockFetchResourceEndpoint.expectedPropertyReceived(EX_PROP_RESOURCE_ID, personUuid);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));
        mockPatientHandlerEndpoint.expectedMessageCount(0);

        producerTemplate.send(URI_PATIENT_ASSOCIATION_HANDLER, exchange);

        mockPatientHandlerEndpoint.assertIsSatisfied();
        assertMessageLogged(Level.WARN, "No associated patient found with uuid: " + personUuid);
    }
}
