package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.TestConstants.LISTENER_URI;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_BASE_URL;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_ID_TYPE_UUID;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_PRODUCT_CONCEPT_ATTRIB_TYPE_UUID;
import static com.mekomsolutions.eip.route.TestConstants.ODOO_QTY_UNITS_CONCEPT_ATTRIB_TYPE_UUID;
import static org.openmrs.eip.mysql.watcher.WatcherTestConstants.URI_MOCK_ERROR_HANDLER;

import org.apache.camel.EndpointInject;
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
@TestPropertySource(properties = TestConstants.PROP_ODOO_PRODUCT_CONCEPT_ATTRIB_TYPE_UUID + "="
        + ODOO_PRODUCT_CONCEPT_ATTRIB_TYPE_UUID)
@TestPropertySource(properties = TestConstants.PROP_ODOO_QTY_UNITS_CONCEPT_ATTRIB_TYPE_UUID + "="
        + ODOO_QTY_UNITS_CONCEPT_ATTRIB_TYPE_UUID)
@TestPropertySource(properties = WatcherConstants.PROP_EVENT_DESTINATIONS + "=" + LISTENER_URI)
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public class OdooIntegrationEventListenerRouteTest extends BaseWatcherRouteTest {
	
	private static final String ROUTE_ID = "odoo-event-listener";
	
	private static final String TABLE_NAME = "orders";
	
	private static final String ORDER_UUID_1 = "16170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	private static final String ORDER_UUID_2 = "26170d8e-d201-4d94-ae89-0be0b0b6d8ba";
	
	@EndpointInject(URI_MOCK_ERROR_HANDLER)
	private MockEndpoint mockErrorHandlerEndpoint;
	
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
	public void shouldSkipSnapshotEvents() throws Exception {
		Event event = createEvent(TABLE_NAME, "1", ORDER_UUID_1, "c");
		event.setSnapshot(true);
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldSkipNonMonitoredTables() throws Exception {
		Event event = createEvent("visit", "1", "some_uuid", "c");
		mockErrorHandlerEndpoint.expectedMessageCount(0);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
}
