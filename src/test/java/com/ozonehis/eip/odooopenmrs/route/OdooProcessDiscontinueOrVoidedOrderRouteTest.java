package com.ozonehis.eip.odooopenmrs.route;

import java.util.Collections;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OdooProcessDiscontinueOrVoidedOrderRouteTest extends BaseOrderOdooRouteTest {

    private static final String ROUTE_ID = "odoo-process-dc-or-voided-order";

    public static final String URI_PROCESS_DC_OR_REV_ORDER = "direct:odoo-process-dc-or-voided-order";

    public static final String EX_PROP_ORDER_LINE = "order-line";

    public static final String EX_PROP_ORDER_LINE_COUNT = "order-line-count";

    @EndpointInject("mock:odoo-manage-order-line")
    private MockEndpoint mockManageOrderLineEndpoint;

    @EndpointInject("mock:odoo-manage-quotation")
    private MockEndpoint mockManageQuotationEndpoint;

    @BeforeEach
    public void setup() throws Exception {
        loadXmlRoutesInCamelDirectory("odoo-process-dc-or-voided-order.xml");

        mockManageOrderLineEndpoint.reset();
        mockManageQuotationEndpoint.reset();

        advise(ROUTE_ID, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint("direct:odoo-manage-order-line")
                        .skipSendToOriginalEndpoint()
                        .to(mockManageOrderLineEndpoint);
                interceptSendToEndpoint("direct:odoo-manage-quotation")
                        .skipSendToOriginalEndpoint()
                        .to(mockManageQuotationEndpoint);
            }
        });
    }

    @Test
    public void shouldProcessOrderWithNoLine() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EX_PROP_ORDER_LINE_COUNT, 2);
        mockManageOrderLineEndpoint.expectedMessageCount(0);
        mockManageQuotationEndpoint.expectedMessageCount(0);

        producerTemplate.send(URI_PROCESS_DC_OR_REV_ORDER, exchange);

        mockManageOrderLineEndpoint.assertIsSatisfied();
        mockManageQuotationEndpoint.assertIsSatisfied();
        Assertions.assertEquals(
                OdooTestConstants.ODOO_OP_UNLINK, exchange.getProperty(OdooTestConstants.EX_PROP_ODOO_OP));
    }

    @Test
    public void shouldRemoveTheOrderLineFromTheQuote() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EX_PROP_ORDER_LINE, Collections.emptyMap());
        exchange.setProperty(EX_PROP_ORDER_LINE_COUNT, 2);
        mockManageOrderLineEndpoint.expectedMessageCount(1);
        mockManageQuotationEndpoint.expectedMessageCount(0);
        mockManageOrderLineEndpoint.expectedPropertyReceived(
                OdooTestConstants.EX_PROP_ODOO_OP, OdooTestConstants.ODOO_OP_UNLINK);

        producerTemplate.send(URI_PROCESS_DC_OR_REV_ORDER, exchange);

        mockManageOrderLineEndpoint.assertIsSatisfied();
        mockManageQuotationEndpoint.assertIsSatisfied();
        Assertions.assertEquals(
                OdooTestConstants.ODOO_OP_UNLINK, exchange.getProperty(OdooTestConstants.EX_PROP_ODOO_OP));
    }

    @Test
    public void shouldCancelTheQuotationIfItHasNoMoreOrderLines() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setProperty(EX_PROP_ORDER_LINE_COUNT, 1);
        mockManageQuotationEndpoint.expectedMessageCount(1);
        mockManageQuotationEndpoint.expectedPropertyReceived(
                OdooTestConstants.EX_PROP_ODOO_OP, OdooTestConstants.ODOO_OP_WRITE);

        producerTemplate.send(URI_PROCESS_DC_OR_REV_ORDER, exchange);

        mockManageQuotationEndpoint.assertIsSatisfied();
    }
}
