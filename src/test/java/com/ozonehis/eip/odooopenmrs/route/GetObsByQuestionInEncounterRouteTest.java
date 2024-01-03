package com.ozonehis.eip.odooopenmrs.route;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetObsByQuestionInEncounterRouteTest extends BaseOdooRouteTest {

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInCamelDirectory(OdooTestConstants.ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC + ".xml");
    }

    @Test
    public void shouldSetBodyToTheMatchingObsIfTheEncounterHasIt() {
        final String qnUuid = "question-concept-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        var expectedObs = singletonMap("concept", singletonMap("uuid", qnUuid));
        var encResource = singletonMap(
                "obs",
                asList(
                        singletonMap("concept", singletonMap("uuid", "test-1")),
                        expectedObs,
                        singletonMap("concept", singletonMap("uuid", "test-2"))));
        exchange.setProperty(OdooTestConstants.EX_PROP_ENC, encResource);
        exchange.setProperty(OdooTestConstants.EX_PROP_QN_CONCEPT_UUID, qnUuid);

        producerTemplate.send(OdooTestConstants.URI_GET_CONCEPT_BY_UUID_FROM_ENC, exchange);

        assertEquals(expectedObs, exchange.getIn().getBody());
    }

    @Test
    public void shouldSetBodyToTheMatchingObsIfTheEncounterHasItAndIsTheLastInTheList() {
        final String qnUuid = "question-concept-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        var expectedObs = singletonMap("concept", singletonMap("uuid", qnUuid));
        var encResource =
                singletonMap("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1")), expectedObs));
        exchange.setProperty(OdooTestConstants.EX_PROP_ENC, encResource);
        exchange.setProperty(OdooTestConstants.EX_PROP_QN_CONCEPT_UUID, qnUuid);

        producerTemplate.send(OdooTestConstants.URI_GET_CONCEPT_BY_UUID_FROM_ENC, exchange);

        assertEquals(expectedObs, exchange.getIn().getBody());
    }

    @Test
    public void shouldSetBodyToNullIfTheEncounterDoesNotHaveIt() {
        Exchange exchange = new DefaultExchange(camelContext);
        Map encResource = singletonMap("obs", asList(singletonMap("concept", singletonMap("uuid", "test-1"))));
        exchange.setProperty(OdooTestConstants.EX_PROP_ENC, encResource);
        exchange.setProperty(OdooTestConstants.EX_PROP_QN_CONCEPT_UUID, "question-concept-uuid");

        producerTemplate.send(OdooTestConstants.URI_GET_CONCEPT_BY_UUID_FROM_ENC, exchange);

        assertNull(exchange.getIn().getBody());
    }

    @Test
    public void shouldIgnoredVoidedObs() {
        final String qnUuid = "question-concept-uuid";
        Exchange exchange = new DefaultExchange(camelContext);
        var expectedObs = new HashMap<>();
        expectedObs.put("concept", singletonMap("uuid", qnUuid));
        expectedObs.put("uuid", qnUuid);
        expectedObs.put("voided", true);
        var encResource = singletonMap("obs", List.of(expectedObs));
        exchange.setProperty(OdooTestConstants.EX_PROP_ENC, encResource);
        exchange.setProperty(OdooTestConstants.EX_PROP_QN_CONCEPT_UUID, qnUuid);

        producerTemplate.send(OdooTestConstants.URI_GET_CONCEPT_BY_UUID_FROM_ENC, exchange);

        assertNull(exchange.getIn().getBody());
    }
}
