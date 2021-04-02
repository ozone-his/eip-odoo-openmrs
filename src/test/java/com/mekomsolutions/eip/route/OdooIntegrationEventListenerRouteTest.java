package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.TestConstants.EX_PROP_ENTITY;
import static com.mekomsolutions.eip.route.TestConstants.EX_PROP_TABLE_REPO_MAP;
import static com.mekomsolutions.eip.route.TestConstants.LISTENER_URI;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
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
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
	@Test
	public void shouldSkipNonMonitoredTables() throws Exception {
		Event event = createEvent("visit", "1", "some_uuid", "c");
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(PROP_EVENT, event);
		
		producerTemplate.send(LISTENER_URI, exchange);
		
		mockErrorHandlerEndpoint.assertIsSatisfied();
		assertNotNull(exchange.getProperty(EX_PROP_TABLE_REPO_MAP));
		assertNull(exchange.getProperty(EX_PROP_ENTITY));
	}
	
}
