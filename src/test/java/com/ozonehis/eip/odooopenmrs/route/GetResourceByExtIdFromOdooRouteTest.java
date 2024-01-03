package com.ozonehis.eip.odooopenmrs.route;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GetResourceByExtIdFromOdooRouteTest extends BaseOdooRouteTest {

    public static final String PARAM_EXT_ID = "externalId";

    @EndpointInject("mock:" + OdooTestConstants.ROUTE_ID_GET_EXT_ID_MAP)
    private MockEndpoint mockGetExtIdMapEndpoint;

    @EndpointInject("mock:" + OdooTestConstants.ROUTE_ID_GET_RES_BY_ID_FROM_ODOO)
    private MockEndpoint mockGetResByIdEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInCamelDirectory(OdooTestConstants.ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO + ".xml");

        mockGetExtIdMapEndpoint.reset();
        mockGetResByIdEndpoint.reset();

        advise(OdooTestConstants.ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint(OdooTestConstants.URI_GET_EXT_ID)
                        .skipSendToOriginalEndpoint()
                        .to(mockGetExtIdMapEndpoint);
                interceptSendToEndpoint(OdooTestConstants.URI_GET_RES_BY_ID_FROM_ODOO)
                        .skipSendToOriginalEndpoint()
                        .to(mockGetResByIdEndpoint);
            }
        });
    }

    @Test
    public void shouldGetTheResourceFromOdooMatchingTheNameAndModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        final int resId = 5;
        final String extId = "test id";
        final String modelName = "res.test";
        Map params = new HashMap();
        params.put(PARAM_EXT_ID, extId);
        params.put(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        exchange.getIn().setBody(params);
        mockGetExtIdMapEndpoint.expectedMessageCount(1);
        mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_EXT_ID, extId);
        mockGetExtIdMapEndpoint.expectedPropertyReceived(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        Map[] expectedExtMaps = new Map[] {singletonMap("res_id", resId)};
        mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedExtMaps));
        Map getResByIdParams = new HashMap();
        getResByIdParams.put(GetResourceByIdFromOdooRouteTest.PARAM_ID, resId);
        getResByIdParams.put(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        final String expectedRes = "{test}";
        mockGetResByIdEndpoint.expectedMessageCount(1);
        mockGetResByIdEndpoint.expectedBodiesReceived(getResByIdParams);
        mockGetResByIdEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedRes));

        producerTemplate.send(OdooTestConstants.URI_GET_RES_BY_EXT_ID_FROM_ODOO, exchange);

        assertEquals(expectedRes, exchange.getIn().getBody());
        mockGetExtIdMapEndpoint.assertIsSatisfied();
        mockGetResByIdEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldReturnNullIfNoResourceIsFoundMatchingTheExternalId() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        final String extId = "test id";
        final String modelName = "res.test";
        Map params = new HashMap();
        params.put(PARAM_EXT_ID, extId);
        params.put(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        exchange.getIn().setBody(params);
        mockGetExtIdMapEndpoint.expectedMessageCount(1);
        mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_EXT_ID, extId);
        mockGetExtIdMapEndpoint.expectedPropertyReceived(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
        mockGetResByIdEndpoint.expectedMessageCount(0);

        producerTemplate.send(OdooTestConstants.URI_GET_RES_BY_EXT_ID_FROM_ODOO, exchange);

        assertNull(exchange.getIn().getBody());
        mockGetExtIdMapEndpoint.assertIsSatisfied();
        mockGetResByIdEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldFailIfMultipleResourceAreFoundMatchingTheExternalId() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        final String extId = "test id";
        final String modelName = "res.test";
        Map params = new HashMap();
        params.put(PARAM_EXT_ID, extId);
        params.put(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        exchange.getIn().setBody(params);
        mockGetExtIdMapEndpoint.expectedMessageCount(1);
        mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_EXT_ID, extId);
        mockGetExtIdMapEndpoint.expectedPropertyReceived(OdooTestConstants.PARAM_MODEL_NAME, modelName);
        Map[] expectedExtMaps = new Map[] {singletonMap("res_id", 1), singletonMap("res_id", 2)};
        mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedExtMaps));
        mockGetResByIdEndpoint.expectedMessageCount(0);

        producerTemplate.send(OdooTestConstants.URI_GET_RES_BY_EXT_ID_FROM_ODOO, exchange);

        mockGetExtIdMapEndpoint.assertIsSatisfied();
        mockGetResByIdEndpoint.assertIsSatisfied();
        assertEquals(
                "Found " + expectedExtMaps.length + " resources(" + modelName + ") in odoo mapped to external id: "
                        + extId,
                getErrorMessage(exchange));
    }
}
