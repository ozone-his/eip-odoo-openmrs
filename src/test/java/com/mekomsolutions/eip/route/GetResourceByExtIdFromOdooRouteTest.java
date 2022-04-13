package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.GetResourceByIdFromOdooRouteTest.PARAM_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.PARAM_MODEL_NAME;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_EXT_ID_MAP;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_RES_BY_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_EXT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_RES_BY_EXT_ID_FROM_ODOO;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_RES_BY_ID_FROM_ODOO;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;

public class GetResourceByExtIdFromOdooRouteTest extends BaseOdooRouteTest {
	
	public static final String PARAM_EXT_ID = "externalId";
	
	@EndpointInject("mock:" + ROUTE_ID_GET_EXT_ID_MAP)
	private MockEndpoint mockGetExtIdMapEndpoint;
	
	@EndpointInject("mock:" + ROUTE_ID_GET_RES_BY_ID_FROM_ODOO)
	private MockEndpoint mockGetResByIdEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetExtIdMapEndpoint.reset();
		mockGetResByIdEndpoint.reset();
		
		advise(ROUTE_ID_GET_RES_BY_EXT_ID_FROM_ODOO, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_EXT_ID).skipSendToOriginalEndpoint().to(mockGetExtIdMapEndpoint);
				interceptSendToEndpoint(URI_GET_RES_BY_ID_FROM_ODOO).skipSendToOriginalEndpoint().to(mockGetResByIdEndpoint);
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
		params.put(PARAM_MODEL_NAME, modelName);
		exchange.getIn().setBody(params);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_EXT_ID, extId);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_MODEL_NAME, modelName);
		Map[] expectedExtMaps = new Map[] { singletonMap("res_id", resId) };
		mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedExtMaps));
		Map getResByIdParams = new HashMap();
		getResByIdParams.put(PARAM_ID, resId);
		getResByIdParams.put(PARAM_MODEL_NAME, modelName);
		final String expectedRes = "{test}";
		mockGetResByIdEndpoint.expectedMessageCount(1);
		mockGetResByIdEndpoint.expectedBodiesReceived(getResByIdParams);
		mockGetResByIdEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedRes));
		
		producerTemplate.send(URI_GET_RES_BY_EXT_ID_FROM_ODOO, exchange);
		
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
		params.put(PARAM_MODEL_NAME, modelName);
		exchange.getIn().setBody(params);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_EXT_ID, extId);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_MODEL_NAME, modelName);
		mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Map[] {}));
		mockGetResByIdEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_GET_RES_BY_EXT_ID_FROM_ODOO, exchange);
		
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
		params.put(PARAM_MODEL_NAME, modelName);
		exchange.getIn().setBody(params);
		mockGetExtIdMapEndpoint.expectedMessageCount(1);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_EXT_ID, extId);
		mockGetExtIdMapEndpoint.expectedPropertyReceived(PARAM_MODEL_NAME, modelName);
		Map[] expectedExtMaps = new Map[] { singletonMap("res_id", 1), singletonMap("res_id", 2) };
		mockGetExtIdMapEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(expectedExtMaps));
		mockGetResByIdEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_GET_RES_BY_EXT_ID_FROM_ODOO, exchange);
		
		mockGetExtIdMapEndpoint.assertIsSatisfied();
		mockGetResByIdEndpoint.assertIsSatisfied();
		assertEquals(
		    "Found " + expectedExtMaps.length + " resources(" + modelName + ") in odoo mapped to external id: " + extId,
		    getErrorMessage(exchange));
	}
	
}
