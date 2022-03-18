package com.mekomsolutions.eip.route;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "odoo.database=" + BaseOdooApiRouteTest.APP_PROP_ODOO_DB)
@TestPropertySource(properties = "odoo.password=" + BaseOdooApiRouteTest.APP_PROP_ODOO_PASS)
public abstract class BaseOdooApiRouteTest extends BaseWatcherRouteTest {
	
	private static final String ODOO_USER_ID_KEY = "routeId-odooUserId";
	
	private static final String RPC_CLIENT_KEY = "routeId-xmlRpcClient";
	
	private static final String RPC_CFG_KEY = "routeId-xmlRpcConfig";
	
	protected static final String APP_PROP_ODOO_DB = "odoo-db";
	
	protected static final String APP_PROP_ODOO_PASS = "odoo-pass";
	
	protected static final Integer USER_ID = 5;
	
	@Mock
	protected XmlRpcClient mockXmlRpcClient;
	
	@Mock
	protected XmlRpcClientConfigImpl mockXmlRpcClientConfig;
	
	@Before
	public void beforeBaseOdooApiRouteTestMethod() {
		AppContext.add(ODOO_USER_ID_KEY, USER_ID);
		AppContext.add(RPC_CLIENT_KEY, mockXmlRpcClient);
		AppContext.add(RPC_CFG_KEY, mockXmlRpcClientConfig);
		
	}
	
	@After
	public void afterBaseOdooApiRouteTestMethod() {
		AppContext.remove(ODOO_USER_ID_KEY);
		AppContext.remove(RPC_CLIENT_KEY);
		AppContext.remove(RPC_CFG_KEY);
	}
	
	protected Exchange buildExchange() {
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(OdooTestConstants.EX_PROP_ODOO_USER_ID_KEY, ODOO_USER_ID_KEY);
		exchange.setProperty(OdooTestConstants.EX_PROP_RPC_CLIENT_KEY, RPC_CLIENT_KEY);
		exchange.setProperty(OdooTestConstants.EX_PROP_RPC_CFG_KEY, RPC_CFG_KEY);
		return exchange;
	}
	
}
