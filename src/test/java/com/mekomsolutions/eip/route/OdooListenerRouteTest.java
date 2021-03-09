package com.mekomsolutions.eip.route;

import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.sql.ResultSet;

import static org.openmrs.eip.mysql.watcher.WatcherTestConstants.URI_MOCK_ERROR_HANDLER;

@Sql(value = {"classpath:test_data.sql"}, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public class OdooListenerRouteTest extends BaseWatcherRouteTest {

    private static final String URI = "direct:odoo-event-listener";

    private static final String TABLE_NAME = "orders";

    private static final String ORDER_UUID = "06170d8e-d201-4d94-ae89-0be0b0b6d8ba";

    @EndpointInject(URI_MOCK_ERROR_HANDLER)
    private MockEndpoint mockErrorHandlerEndpoint;

    @Before
    public void setup() {
        mockErrorHandlerEndpoint.reset();
    }

    @Test
    public void shouldSkipSnapshotEvents() throws Exception {
        Event event = createEvent(TABLE_NAME, "1", ORDER_UUID, "c");
        event.setSnapshot(true);
        mockErrorHandlerEndpoint.expectedMessageCount(0);

        producerTemplate.sendBodyAndProperty(URI, null, PROP_EVENT, event);

        mockErrorHandlerEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldSkipDeleteEvents() throws Exception {
        Event event = createEvent(TABLE_NAME, "1", ORDER_UUID, "d");
        mockErrorHandlerEndpoint.expectedMessageCount(0);

        producerTemplate.sendBodyAndProperty(URI, null, PROP_EVENT, event);

        mockErrorHandlerEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldSkipEventsThatAreNotForOrders() throws Exception {
        Event event = createEvent("visit", "1", "visit-uuid", "c");
        mockErrorHandlerEndpoint.expectedMessageCount(0);

        producerTemplate.sendBodyAndProperty(URI, null, PROP_EVENT, event);

        mockErrorHandlerEndpoint.assertIsSatisfied();
    }

    @Test
    public void shouldPushTheOrderToOdoo() throws Exception {
        Event event = createEvent(TABLE_NAME, "1", ORDER_UUID, "c");
        mockErrorHandlerEndpoint.expectedMessageCount(0);
        
        producerTemplate.sendBodyAndProperty(URI, null, PROP_EVENT, event);

        mockErrorHandlerEndpoint.assertIsSatisfied();
    }

}
