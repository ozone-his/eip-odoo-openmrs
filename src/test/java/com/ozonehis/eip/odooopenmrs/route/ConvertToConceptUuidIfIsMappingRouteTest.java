package com.ozonehis.eip.odooopenmrs.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openmrs.eip.Constants.EX_PROP_CONCEPT_CODE;
import static org.openmrs.eip.Constants.EX_PROP_CONCEPT_SOURCE;

import java.util.Collections;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConvertToConceptUuidIfIsMappingRouteTest extends BaseOdooRouteTest {

    private static final String ROUTE_ID = "convert-to-concept-uuid-if-is-mapping";

    @EndpointInject("mock:get-concept-by-mapping-from-openmrs")
    private MockEndpoint mockGetConceptByMapEndpoint;

    @BeforeEach
    public void setup() throws Exception {

        loadXmlRoutesInCamelDirectory(ROUTE_ID + ".xml");

        mockGetConceptByMapEndpoint.reset();

        advise(ROUTE_ID, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint(OdooTestConstants.URI_GET_CONCEPT_BY_MAPPING)
                        .skipSendToOriginalEndpoint()
                        .to(mockGetConceptByMapEndpoint);
            }
        });
    }

    @Test
    public void shouldConvertAConceptMappingToAUuid() throws Exception {
        final String source = "test-source";
        final String code = "test-code";
        final String expectedConceptUuid = "test-uuid";
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(source + ":" + code);
        mockGetConceptByMapEndpoint.expectedMessageCount(1);
        mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_CONCEPT_SOURCE, source);
        mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_CONCEPT_CODE, code);
        mockGetConceptByMapEndpoint.whenAnyExchangeReceived(e -> {
            e.getIn().setBody(Collections.singletonMap("uuid", expectedConceptUuid));
        });

        producerTemplate.send(OdooTestConstants.URI_CONVERT_TO_CONCEPT_UUID, exchange);

        mockGetConceptByMapEndpoint.assertIsSatisfied();
        assertEquals(expectedConceptUuid, exchange.getIn().getBody(String.class));
    }

    @Test
    public void shouldFailIfNoConceptIsFoundMatchingTheMapping() throws Exception {
        final String source = "test-source";
        final String code = "test-code";
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(source + ":" + code);
        mockGetConceptByMapEndpoint.expectedMessageCount(1);
        mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_CONCEPT_SOURCE, source);
        mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_CONCEPT_CODE, code);
        mockGetConceptByMapEndpoint.whenAnyExchangeReceived(e -> {
            e.getIn().setBody(null);
        });

        producerTemplate.send(OdooTestConstants.URI_CONVERT_TO_CONCEPT_UUID, exchange);

        mockGetConceptByMapEndpoint.assertIsSatisfied();
        assertEquals(
                "No concept found with mapping matching source: " + source + " and code: " + code,
                getErrorMessage(exchange));
    }

    @Test
    public void shouldReturnTheOriginalValueForANonValidConceptMapping() throws Exception {
        final String testConceptUuid = "test-uuid";
        final Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(testConceptUuid);
        mockGetConceptByMapEndpoint.expectedMessageCount(0);

        producerTemplate.send(OdooTestConstants.URI_CONVERT_TO_CONCEPT_UUID, exchange);

        mockGetConceptByMapEndpoint.assertIsSatisfied();
        assertEquals(testConceptUuid, exchange.getIn().getBody(String.class));
    }
}
