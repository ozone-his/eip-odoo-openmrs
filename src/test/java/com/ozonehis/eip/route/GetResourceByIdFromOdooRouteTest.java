package com.ozonehis.eip.route;

import static com.ozonehis.eip.route.OdooTestConstants.ODOO_OP_SEARCH_READ;
import static com.ozonehis.eip.route.OdooTestConstants.ODOO_RPC_METHOD;
import static com.ozonehis.eip.route.OdooTestConstants.URI_GET_RES_BY_ID_FROM_ODOO;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.junit.Assert;
import org.junit.Test;

public class GetResourceByIdFromOdooRouteTest extends BaseOdooApiRouteTest {
	
	public static final String MODEL_NAME_GROUPS = "res.groups";
	
	public static final String PARAM_ID = "id";
	
	public static final String PARAM_MODEL_NAME = "modelName";
	
	@Test
	public void shouldGetTheResourceFromOdooMatchingTheNameAndModel() throws Exception {
		Exchange exchange = buildExchange();
		final int groupId = 3;
		Map params = new HashMap();
		params.put(PARAM_ID, groupId);
		params.put(PARAM_MODEL_NAME, MODEL_NAME_GROUPS);
		exchange.getIn().setBody(params);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add(MODEL_NAME_GROUPS);
		rpcArgs.add(ODOO_OP_SEARCH_READ);
		rpcArgs.add(singletonList(singletonList(asList("id", "=", groupId))));
		final Object expectedItem = "test";
		when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs))
		        .thenReturn(new Object[] { expectedItem });
		
		producerTemplate.send(URI_GET_RES_BY_ID_FROM_ODOO, exchange);
		
		assertEquals(expectedItem, exchange.getIn().getBody());
	}
	
	@Test
	public void shouldSetTheBodyToNullIfNoMatchIsFound() throws Exception {
		Exchange exchange = buildExchange();
		final int groupId = 3;
		Map params = new HashMap();
		params.put(PARAM_ID, groupId);
		params.put(PARAM_MODEL_NAME, MODEL_NAME_GROUPS);
		exchange.getIn().setBody(params);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add(MODEL_NAME_GROUPS);
		rpcArgs.add(ODOO_OP_SEARCH_READ);
		rpcArgs.add(singletonList(singletonList(asList("id", "=", groupId))));
		when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs)).thenReturn(new Object[] {});
		
		producerTemplate.send(URI_GET_RES_BY_ID_FROM_ODOO, exchange);
		
		Assert.assertNull(exchange.getIn().getBody());
	}
	
}
