package com.ozonehis.eip.odooopenmrs.route;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = OdooTestConstants.APP_PROP_NAME_ID_TYPE_UUID + "=" + GetHsuIdRouteTest.HSU_ID_TYPE_UUID)
public class GetHsuIdRouteTest extends BaseOdooRouteTest {

    protected static final String HSU_ID_TYPE_UUID = "hsu-id-type-uuid";

    @Test
    public void shouldReturnNullIfThePatientHasNoIdentifier() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(singletonMap("identifiers", Collections.emptyList()));

        producerTemplate.send(OdooTestConstants.URI_GET_HSU_ID, exchange);

        assertNull(exchange.getIn().getBody());
        assertNull(getException(exchange));
    }

    @Test
    public void shouldReturnNullIfNoMatchIsFound() {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn()
                .setBody(singletonMap(
                        "identifiers", asList(singletonMap("identifierType", singletonMap("uuid", "other-id-type")))));

        producerTemplate.send(OdooTestConstants.URI_GET_HSU_ID, exchange);

        assertNull(exchange.getIn().getBody());
        assertNull(getException(exchange));
    }

    @Test
    public void shouldReturnTheHSUIdentifier() {
        final String expectedHsuId = "hsu id";
        var hsuId = new HashMap<>();
        hsuId.put("identifier", expectedHsuId);
        hsuId.put("identifierType", singletonMap("uuid", HSU_ID_TYPE_UUID));
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn()
                .setBody(singletonMap(
                        "identifiers",
                        asList(
                                singletonMap("identifierType", singletonMap("uuid", "other-id-type")),
                                hsuId,
                                singletonMap("identifierType", singletonMap("uuid", "other-id-type")))));

        producerTemplate.send(OdooTestConstants.URI_GET_HSU_ID, exchange);

        assertEquals(expectedHsuId, exchange.getIn().getBody());
        assertNull(getException(exchange));
    }
}
