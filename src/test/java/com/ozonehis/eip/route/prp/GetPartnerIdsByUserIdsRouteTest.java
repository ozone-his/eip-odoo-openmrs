package com.ozonehis.eip.route.prp;

import static com.ozonehis.eip.route.OdooTestConstants.ODOO_OP_SEARCH_READ;
import static com.ozonehis.eip.route.OdooTestConstants.ODOO_RPC_METHOD;
import static com.ozonehis.eip.route.OdooTestConstants.URI_GET_PARTNERS_BY_USERS;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.junit.Test;

import com.ozonehis.eip.route.BaseOdooApiRouteTest;

public class GetPartnerIdsByUserIdsRouteTest extends BaseOdooApiRouteTest {
	
	public static final String PARAM_USER_IDS = "userIds";
	
	@Test
	public void shouldGetThePartnersForTheSpecifiedUserIds() throws Exception {
		final List<Integer> userIds = asList(1, 2);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add("res.users");
		rpcArgs.add(ODOO_OP_SEARCH_READ);
		rpcArgs.add(singletonList(singletonList(asList("id", "in", userIds))));
		rpcArgs.add(singletonMap("fields", singletonList("partner_id")));
		Exchange exchange = buildExchange();
		exchange.getIn().setBody(singletonMap(PARAM_USER_IDS, userIds));
		final Object expectedResp = "test";
		when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs)).thenReturn(expectedResp);
		
		producerTemplate.send(URI_GET_PARTNERS_BY_USERS, exchange);
		
		assertEquals(expectedResp, exchange.getIn().getBody());
	}
	
}
