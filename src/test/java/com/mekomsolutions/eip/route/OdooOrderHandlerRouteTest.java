package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.TestConstants.EX_PROP_ODOO_ID;
import static com.mekomsolutions.eip.route.TestConstants.EX_PROP_ODOO_USER_ID;
import static com.mekomsolutions.eip.route.TestConstants.LISTENER_URI;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_ID_TYPE_UUID;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_PRODUCT_CONCEPT_ATTRIB_TYPE_UUID;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_QTY_UNITS_CONCEPT_ATTRIB_TYPE_UUID;
import static com.mekomsolutions.eip.route.TestConstants.URI_ODOO_AUTH;
import static com.mekomsolutions.eip.route.TestConstants.URI_ODOO_CREATE_CUSTOMER;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.WatcherConstants;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@TestPropertySource(properties = TestConstants.PROP_ODOO_ID_TYPE_UUID + "=" + ODOO_ID_TYPE_UUID)
@TestPropertySource(properties = TestConstants.PROP_ODOO_PRODUCT_CONCEPT_ATTRIB_TYPE_UUID + "=" + ODOO_PRODUCT_CONCEPT_ATTRIB_TYPE_UUID)
@TestPropertySource(properties = TestConstants.PROP_ODOO_QTY_UNITS_CONCEPT_ATTRIB_TYPE_UUID + "=" + ODOO_QTY_UNITS_CONCEPT_ATTRIB_TYPE_UUID)
@TestPropertySource(properties = WatcherConstants.PROP_EVENT_DESTINATIONS + "=" + LISTENER_URI)
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public class OdooOrderHandlerRouteTest extends BaseWatcherRouteTest {
	
	private static final String ROUTE_ID = "odoo-order-handler";
	
	private static final String TABLE_NAME = "orders";
	
	private static final String ORDER_UUID_1 = "16170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	private static final String ORDER_UUID_2 = "26170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	@EndpointInject("mock:odoo-auth")
	private MockEndpoint mockOdooAuthEndpoint;
	
	@EndpointInject("mock:create-customer")
	private MockEndpoint mockCreateCustomerEndpoint;
	
	@Before
	public void setup() {
		mockOdooAuthEndpoint.reset();
		mockErrorHandlerEndpoint.reset();
		mockCreateCustomerEndpoint.reset();
	}

	@Test
	public void shouldSkipDeleteEvents() throws Exception {
		Event event = createEvent(TABLE_NAME, "1", ORDER_UUID_1, "d");
		mockErrorHandlerEndpoint.expectedMessageCount(0);

		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);

		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldUseTheExistingOdooIdentifierWhenPushingOrderToOdoo() throws Exception {
		Event event = createEvent(TABLE_NAME, "1", ORDER_UUID_1, "c");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
		Assert.assertEquals("12345", exchange.getProperty(EX_PROP_ODOO_ID));
	}
	
	@Test
	public void shouldAuthenticateWithOdooAndCreateTheCustomerWithNoOdooId() throws Exception {
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_ODOO_AUTH).skipSendToOriginalEndpoint().to(mockOdooAuthEndpoint);
				interceptSendToEndpoint(URI_ODOO_CREATE_CUSTOMER).skipSendToOriginalEndpoint()
				        .to(mockCreateCustomerEndpoint);
			}
		});

		Event event = createEvent(TABLE_NAME, "1", "26170d8e-d201-4d94-ae89-0be0b0b6d8ba", "c");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		mockOdooAuthEndpoint.expectedMessageCount(1);
		mockCreateCustomerEndpoint.expectedMessageCount(1);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockOdooAuthEndpoint.assertIsSatisfied();
		mockCreateCustomerEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldCreateTheCustomerWithNoOdooIdAndNotAuthenticateWithOdooIfAlreadyLoggedIn() throws Exception {
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_ODOO_CREATE_CUSTOMER).skipSendToOriginalEndpoint()
				        .to(mockCreateCustomerEndpoint);
			}
		});
		
		Event event = createEvent(TABLE_NAME, "2", ORDER_UUID_2, "c");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		mockCreateCustomerEndpoint.expectedMessageCount(1);
		mockOdooAuthEndpoint.expectedMessageCount(0);
		
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		exchange.setProperty(EX_PROP_ODOO_USER_ID, 120);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockOdooAuthEndpoint.assertIsSatisfied();
		mockCreateCustomerEndpoint.assertIsSatisfied();
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
}
