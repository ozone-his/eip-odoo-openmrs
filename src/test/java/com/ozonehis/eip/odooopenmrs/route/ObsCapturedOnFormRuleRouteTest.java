package com.ozonehis.eip.odooopenmrs.route;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import java.util.Map;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ObsCapturedOnFormRuleRouteTest extends BaseOdooRouteTest {

    private static final String ROUTE_ID = "obs-captured-on-form-rule";

    public static final String EX_PROP_OBS = "obs";

    public static final String EX_PROP_FORM_UUID = "formUuid";

    @EndpointInject(OdooTestConstants.URI_MOCK_GET_ENTITY_BY_UUID)
    private MockEndpoint mockFetchResourceEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInCamelDirectory(ROUTE_ID + ".xml");

        mockFetchResourceEndpoint.reset();

        advise(ROUTE_ID, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint(OdooTestConstants.URI_GET_ENTITY_BY_UUID)
                        .skipSendToOriginalEndpoint()
                        .to(mockFetchResourceEndpoint);
            }
        });
    }

    @Test
    public void shouldSetBodyToTrueIfTheObsWasRecordedOnTheFormWithTheSpecifiedUuid() throws Exception {
        final String encounterUuid = "enc-uuid";
        final String formUuid = "form-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        Map encResource = singletonMap("form", singletonMap("uuid", formUuid));
        Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
        exchange.setProperty(EX_PROP_OBS, obsResource);
        exchange.setProperty(EX_PROP_FORM_UUID, formUuid);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_NAME, "encounter");
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_ID, encounterUuid);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RES_REP, "full");
        final String encJson = mapper.writeValueAsString(encResource);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));

        producerTemplate.send(OdooTestConstants.URI_OBS_CAPTURED_ON_FORM, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        assertTrue(exchange.getIn().getBody(Boolean.class));
    }

    @Test
    public void shouldSetBodyToFalseIfTheObsWasRecordedOnAFormWithADifferentUuid() throws Exception {
        final String encounterUuid = "enc-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        Map encResource = singletonMap("form", singletonMap("uuid", "another-form-uuid"));
        Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
        exchange.setProperty(EX_PROP_OBS, obsResource);
        exchange.setProperty(EX_PROP_FORM_UUID, "form-uuid");
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_NAME, "encounter");
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_ID, encounterUuid);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RES_REP, "full");
        final String encJson = mapper.writeValueAsString(encResource);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(encJson));

        producerTemplate.send(OdooTestConstants.URI_OBS_CAPTURED_ON_FORM, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        assertFalse(exchange.getIn().getBody(Boolean.class));
    }

    @Test
    public void shouldSetBodyToFalseForAFormLessEncounter() throws Exception {
        final String encounterUuid = "enc-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(encounterUuid);
        Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
        exchange.setProperty(EX_PROP_OBS, obsResource);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_NAME, "encounter");
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_ID, encounterUuid);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(emptyMap()));

        producerTemplate.send(OdooTestConstants.URI_OBS_CAPTURED_ON_FORM, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        assertMessageLogged(Level.INFO, "Obs encounter does not belong to any form");
        assertFalse(exchange.getIn().getBody(Boolean.class));
    }

    @Test
    public void shouldSetBodyToFalseForAnEncounterLessObs() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EX_PROP_OBS, emptyMap());
        mockFetchResourceEndpoint.expectedMessageCount(0);

        producerTemplate.send(OdooTestConstants.URI_OBS_CAPTURED_ON_FORM, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        assertFalse(exchange.getIn().getBody(Boolean.class));
        assertMessageLogged(Level.INFO, "Obs does not belong to any encounter");
    }

    @Test
    public void shouldFailIfNoEncounterIsFoundMatchingTheEncounterUuid() throws Exception {
        final String encounterUuid = "enc-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(encounterUuid);
        Map obsResource = singletonMap("encounter", singletonMap("uuid", encounterUuid));
        exchange.setProperty(EX_PROP_OBS, obsResource);
        mockFetchResourceEndpoint.expectedMessageCount(1);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_IS_SUBRESOURCE, false);
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_NAME, "encounter");
        mockFetchResourceEndpoint.expectedPropertyReceived(OdooTestConstants.EX_PROP_RESOURCE_ID, encounterUuid);
        mockFetchResourceEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(null));

        producerTemplate.send(OdooTestConstants.URI_OBS_CAPTURED_ON_FORM, exchange);

        mockFetchResourceEndpoint.assertIsSatisfied();
        assertEquals("No encounter found with uuid: " + encounterUuid, getErrorMessage(exchange));
    }
}
