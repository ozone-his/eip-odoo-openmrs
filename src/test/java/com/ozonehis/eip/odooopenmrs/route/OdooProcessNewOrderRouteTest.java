package com.ozonehis.eip.odooopenmrs.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import ch.qos.logback.classic.Level;
import java.util.Collections;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.mysql.watcher.Event;

public class OdooProcessNewOrderRouteTest extends BaseOrderOdooRouteTest {

    private static final String ROUTE_ID = "odoo-process-new-order";

    public static final String URI_PROCESS_NEW_ORDER = "direct:odoo-process-new-order";

    public static final String EX_PROP_ORDER_LINE = "order-line";

    @EndpointInject("mock:odoo-manage-order-line")
    private MockEndpoint mockManageOrderLineEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInCamelDirectory("odoo-process-new-order.xml");

        mockManageOrderLineEndpoint.reset();

        advise(ROUTE_ID, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint("direct:odoo-manage-order-line")
                        .skipSendToOriginalEndpoint()
                        .to(mockManageOrderLineEndpoint);
            }
        });
    }

    @Test
    public void shouldFailIfThereIsAnExistingOrderLineWhenProcessingAnOrderEvent() {
        Event event = createEvent("orders", "1", "order-uuid", "c");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        exchange.setProperty(EX_PROP_ORDER_LINE, Collections.emptyMap());

        producerTemplate.send(URI_PROCESS_NEW_ORDER, exchange);

        assertEquals(
                "There is already an existing order line on the quotation for the same orderable in odoo",
                getErrorMessage(exchange));
    }

    @Test
    public void shouldIgnoreDrugOrderTableEvent() throws Exception {
        final String tableName = "drug_order";
        Event event = createEvent(tableName, "1", "order-uuid", "c");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        mockManageOrderLineEndpoint.expectedMessageCount(0);

        producerTemplate.send(URI_PROCESS_NEW_ORDER, exchange);

        mockManageOrderLineEndpoint.assertIsSatisfied();
        assertMessageLogged(Level.INFO, "Ignoring event for new order from subclass table: " + tableName);
    }

    @Test
    public void shouldIgnoreTestOrderTableEvent() throws Exception {
        final String tableName = "test_order";
        Event event = createEvent(tableName, "1", "order-uuid", "c");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        mockManageOrderLineEndpoint.expectedMessageCount(0);

        producerTemplate.send(URI_PROCESS_NEW_ORDER, exchange);

        mockManageOrderLineEndpoint.assertIsSatisfied();
        assertMessageLogged(Level.INFO, "Ignoring event for new order from subclass table: " + tableName);
    }

    @Test
    public void shouldProcessOrderTableEvent() throws Exception {
        Event event = createEvent("orders", "1", "order-uuid", "c");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(PROP_EVENT, event);
        mockManageOrderLineEndpoint.expectedMessageCount(1);
        mockManageOrderLineEndpoint.expectedPropertyReceived(
                OdooTestConstants.EX_PROP_ODOO_OP, OdooTestConstants.ODOO_OP_CREATE);

        producerTemplate.send(URI_PROCESS_NEW_ORDER, exchange);

        mockManageOrderLineEndpoint.assertIsSatisfied();
    }
}
