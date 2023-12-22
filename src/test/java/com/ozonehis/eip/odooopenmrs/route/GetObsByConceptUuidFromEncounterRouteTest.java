package com.ozonehis.eip.odooopenmrs.route;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

public class GetObsByConceptUuidFromEncounterRouteTest extends BaseOdooRouteTest {

    private static final String URI = "direct:" + OdooTestConstants.ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC;

    public static final String EX_PROP_OBS_QN_CONCEPT_UUID = "questionConceptUuid";

    public static final String EX_PROP_ENC = "encounter";

    @Test
    public void shouldReturnNullIfTheEncounterHasNoObs() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EX_PROP_ENC, singletonMap("obs", emptyList()));

        producerTemplate.send(URI, exchange);

        assertNull(exchange.getIn().getBody());
        assertNull(getException(exchange));
    }

    @Test
    public void shouldReturnNullIfNoMatchIsFound() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(
                EX_PROP_ENC,
                singletonMap("obs", List.of(singletonMap("concept", singletonMap("uuid", "concept-uuid1")))));

        producerTemplate.send(URI, exchange);

        assertNull(exchange.getIn().getBody());
    }

    @Test
    public void shouldReturnTheConceptWithAMatchingConceptUuid() {
        final String conceptUuid = "concept-uuid";
        var expectObs = singletonMap("concept", singletonMap("uuid", conceptUuid));
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EX_PROP_OBS_QN_CONCEPT_UUID, conceptUuid);
        exchange.setProperty(
                EX_PROP_ENC,
                singletonMap(
                        "obs",
                        asList(
                                singletonMap("concept", singletonMap("uuid", "concept-uuid1")),
                                expectObs,
                                singletonMap("concept", singletonMap("uuid", "concept-uuid2")))));

        producerTemplate.send(URI, exchange);

        assertEquals(expectObs, exchange.getIn().getBody());
    }
}
