package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_SEARCH_READ;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_RPC_METHOD;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_RES_BY_NAME_FROM_ODOO;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.camel.Exchange;
import org.junit.Assert;
import org.junit.Test;

public class GetResourceByNameFromOdooRouteTest extends BaseOdooApiRouteTest {
	
	private static final String MODEL_NAME = "res.groups";
	
	public static final String EX_PROP_NAME = "name";
	
	public static final String EX_PROP_MODEL_NAME = "modelName";
	
	@Test
	public void shouldGetTheResourceFromOdooMatchingTheNameAndModel() throws Exception {
		Exchange exchange = buildExchange();
		final String groupName = "Test group name";
		exchange.setProperty(EX_PROP_NAME, groupName);
		exchange.setProperty(EX_PROP_MODEL_NAME, MODEL_NAME);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add(MODEL_NAME);
		rpcArgs.add(ODOO_OP_SEARCH_READ);
		rpcArgs.add(singletonList(singletonList(asList("name", "=", groupName))));
		final Object expectedItem = "test";
		when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs))
		        .thenReturn(new Object[] { expectedItem });
		
		producerTemplate.send(URI_GET_RES_BY_NAME_FROM_ODOO, exchange);
		
		assertEquals(expectedItem, exchange.getIn().getBody());
	}
	
	@Test
	public void shouldSetTheBodyToNullIfNoMatchIsFound() throws Exception {
		Exchange exchange = buildExchange();
		final String groupName = "Test group name";
		exchange.setProperty(EX_PROP_NAME, groupName);
		exchange.setProperty(EX_PROP_MODEL_NAME, MODEL_NAME);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add(MODEL_NAME);
		rpcArgs.add(ODOO_OP_SEARCH_READ);
		rpcArgs.add(singletonList(singletonList(asList("name", "=", groupName))));
		when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs)).thenReturn(new Object[] {});
		
		producerTemplate.send(URI_GET_RES_BY_NAME_FROM_ODOO, exchange);
		
		Assert.assertNull(exchange.getIn().getBody());
	}
	
	@Test
	public void shouldFailIfMultipleMatchesAreFound() throws Exception {
		Exchange exchange = buildExchange();
		final String groupName = "Test group name";
		exchange.setProperty(EX_PROP_NAME, groupName);
		exchange.setProperty(EX_PROP_MODEL_NAME, MODEL_NAME);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add(MODEL_NAME);
		rpcArgs.add(ODOO_OP_SEARCH_READ);
		rpcArgs.add(singletonList(singletonList(asList("name", "=", groupName))));
		when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs))
		        .thenReturn(new Object[] { "item1", "item2" });
		
		producerTemplate.send(URI_GET_RES_BY_NAME_FROM_ODOO, exchange);
		
		assertEquals("Found 2 resources (" + MODEL_NAME + ") in odoo with name: " + groupName, getErrorMessage(exchange));
	}
	
}
