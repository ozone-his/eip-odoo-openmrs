package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.TestConstants.LISTENER_URI;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@TestPropertySource(properties = "camel.springboot.route-filter-include-pattern=direct:odoo-event-listener")
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public class OdooIntegrationEventListenerRouteTest extends BaseWatcherRouteTest {
	
	private static final String ROUTE_ID = "odoo-event-listener";
	
	@EndpointInject("mock:odoo-auth")
	private MockEndpoint mockOdooAuthEndpoint;
	
	@EndpointInject("mock:odoo-order-handler")
	private MockEndpoint mockOdooOrderHandlerEndpoint;
	
	@EndpointInject("mock:odoo-patient-handler")
	private MockEndpoint mockPatientHandlerEndpoint;
	
	@EndpointInject("mock:odoo-person-name-handler")
	private MockEndpoint mockPersonNameEndpoint;
	
	@EndpointInject("mock:odoo-person-address-handler")
	private MockEndpoint mockPersonAddressHandlerEndpoint;
	
	@Before
	public void setup() {
		mockErrorHandlerEndpoint.reset();
		mockOdooAuthEndpoint.reset();
		mockOdooOrderHandlerEndpoint.reset();
		mockPatientHandlerEndpoint.reset();
		mockPersonNameEndpoint.reset();
		mockPersonAddressHandlerEndpoint.reset();
		
		mockErrorHandlerEndpoint.expectedMessageCount(0);
	}
	
	@Test
	public void shouldSkipSnapshotEvents() throws Exception {
		Event event = createEvent("orders", "1", "some-uuid", "c");
		event.setSnapshot(true);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldSkipNonMonitoredTables() throws Exception {
		Event event = createEvent("visit", "1", "some_uuid", "c");
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, event);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldCallTheErrorHandlerInCaseOfErrors() throws Exception {
		mockErrorHandlerEndpoint.expectedMessageCount(1);
		
		producerTemplate.sendBodyAndProperty(LISTENER_URI, null, PROP_EVENT, null);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
	}
	
}
