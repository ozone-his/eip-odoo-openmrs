package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.TestConstants.LISTENER_URI;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_BASE_URL;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_ID_TYPE_UUID;
import static org.openmrs.eip.mysql.watcher.WatcherTestConstants.URI_MOCK_ERROR_HANDLER;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.WatcherConstants;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@TestPropertySource(properties = TestConstants.PROP_ODOO_BASE_URL + "=" + ODOO_BASE_URL)
@TestPropertySource(properties = TestConstants.PROP_ODOO_ID_TYPE_UUID + "=" + ODOO_ID_TYPE_UUID)
@TestPropertySource(properties = WatcherConstants.PROP_EVENT_DESTINATIONS + "=" + LISTENER_URI)
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public class OdooListenerRouteTest extends BaseWatcherRouteTest {
	
	private static final String TABLE_NAME = "orders";
	
	private static final String ORDER_UUID = "06170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	@EndpointInject(URI_MOCK_ERROR_HANDLER)
	private MockEndpoint mockErrorHandlerEndpoint;
	
	@EndpointInject("mock:http")
	private MockEndpoint mockHttpEndpoint;
	
	@Before
	public void setup() {
		mockHttpEndpoint.reset();
		mockErrorHandlerEndpoint.reset();
	}
	
	@Test
	public void shouldSkipSnapshotEvents() throws Exception {
		Event event = createEvent(TABLE_NAME, "1", ORDER_UUID, "c");
		event.setSnapshot(true);
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldSkipDeleteEvents() throws Exception {
		Event event = createEvent(TABLE_NAME, "1", ORDER_UUID, "d");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldSkipEventsThatAreNotForOrders() throws Exception {
		Event event = createEvent("visit", "1", "visit-uuid", "c");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldUseTheExistingOdooIdentifierWhenPushingOrderToOdoo() throws Exception {
		mockHttpEndpoint.expectedPropertyReceived("odoo-id", "12345");
		camelContext.adviceWith(camelContext.getRouteDefinition("odoo-event-listener"), new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(ODOO_BASE_URL).skipSendToOriginalEndpoint().to(mockHttpEndpoint);
			}
		});
		
		Event event = createEvent(TABLE_NAME, "1", ORDER_UUID, "c");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		mockHttpEndpoint.expectedMessageCount(1);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
		mockHttpEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldFailIfTheAuthenticateCredentialsAreWrong() throws Exception {
		
	}
	
}
