package com.ozonehis.eip.odooopenmrs.route;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GetResourceByNameFromOdooRouteTest extends BaseOdooApiRouteTest {

    public static final String EX_PROP_NAME = "name";

    public static final String EX_PROP_MODEL_NAME = "modelName";

    private static final String ODOO_USER_ID_KEY = "routeId-odooUserId";

    private static final String RPC_CLIENT_KEY = "routeId-xmlRpcClient";

    private static final String RPC_CFG_KEY = "routeId-xmlRpcConfig";

    protected static final String APP_PROP_ODOO_DB = "odoo-db";

    protected static final String APP_PROP_ODOO_PASS = "odoo-pass";

    protected static final Integer USER_ID = 5;

    @Test
    public void shouldGetTheResourceFromOdooMatchingTheNameAndModel() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        final String groupName = "Test group name";
        exchange.setProperty(EX_PROP_NAME, groupName);
        exchange.setProperty(EX_PROP_MODEL_NAME, OdooTestConstants.MODEL_NAME_GROUPS);
        var rpcArgs = new ArrayList<>();
        rpcArgs.add(APP_PROP_ODOO_DB);
        rpcArgs.add(USER_ID);
        rpcArgs.add(APP_PROP_ODOO_PASS);
        rpcArgs.add(OdooTestConstants.MODEL_NAME_GROUPS);
        rpcArgs.add(OdooTestConstants.ODOO_OP_SEARCH_READ);
        rpcArgs.add(singletonList(singletonList(asList("name", "=", groupName))));
        Object expectedItem = "test";
        exchange.getIn().setBody(expectedItem);

        producerTemplate.send(OdooTestConstants.URI_GET_RES_BY_NAME_FROM_ODOO, exchange);

        assertNotNull(exchange);
        assertEquals(expectedItem, exchange.getMessage().getBody(String.class));
    }

    @Test
    public void shouldSetTheBodyToNullIfNoMatchIsFound() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        final String groupName = "Test group name";
        exchange.setProperty(EX_PROP_NAME, groupName);
        exchange.setProperty(EX_PROP_MODEL_NAME, OdooTestConstants.MODEL_NAME_GROUPS);
        final var rpcArgs = new ArrayList<>();
        rpcArgs.add(APP_PROP_ODOO_DB);
        rpcArgs.add(USER_ID);
        rpcArgs.add(APP_PROP_ODOO_PASS);
        rpcArgs.add(OdooTestConstants.MODEL_NAME_GROUPS);
        rpcArgs.add(OdooTestConstants.ODOO_OP_SEARCH_READ);
        rpcArgs.add(singletonList(singletonList(asList("name", "=", groupName))));

        producerTemplate.send(OdooTestConstants.URI_GET_RES_BY_NAME_FROM_ODOO, exchange);

        assertNull(exchange.getIn().getBody());
    }

    @Test
    public void shouldFailIfMultipleMatchesAreFound() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        final String groupName = "Test group name";
        exchange.setProperty(EX_PROP_NAME, groupName);
        exchange.setProperty(EX_PROP_MODEL_NAME, OdooTestConstants.MODEL_NAME_GROUPS);
        final var rpcArgs = new ArrayList<>();
        rpcArgs.add(APP_PROP_ODOO_DB);
        rpcArgs.add(USER_ID);
        rpcArgs.add(APP_PROP_ODOO_PASS);
        rpcArgs.add(OdooTestConstants.MODEL_NAME_GROUPS);
        rpcArgs.add(OdooTestConstants.ODOO_OP_SEARCH_READ);
        rpcArgs.add(singletonList(singletonList(asList("name", "=", groupName))));

        try {
            producerTemplate.send(OdooTestConstants.URI_GET_RES_BY_NAME_FROM_ODOO, exchange);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Found 2 resources"));
            assertEquals(
                    "Found 2 resources (" + OdooTestConstants.MODEL_NAME_GROUPS + ") in odoo with name: " + groupName,
                   e.getMessage());
        }
    }
}
