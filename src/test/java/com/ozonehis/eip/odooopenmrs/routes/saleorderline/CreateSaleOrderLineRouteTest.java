package com.ozonehis.eip.odooopenmrs.routes.saleorderline;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.Endpoint;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.fhir.Constants;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@UseAdviceWith
public class CreateSaleOrderLineRouteTest extends CamelSpringTestSupport {

    private static final String CREATE_SALE_ORDER_LINE_ROUTE = "direct:odoo-create-sale-order-line-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new CreateSaleOrderLineRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("odoo-create-sale-order-line-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://create/sale.order.line").replace().to("mock:create-sale-order-line");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_SALE_ORDER_LINE_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateSaleOrderLine() throws Exception {
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        saleOrderLine.setSaleOrderLineName("Aspirin 81mg");
        saleOrderLine.setSaleOrderLineOrderId("12345");
        saleOrderLine.setSaleOrderLineProductId("67890");

        Map<String, Object> createHeaders = new HashMap<>();
        createHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "c");

        // Expectations
        MockEndpoint mockCreateSaleOrderLineEndpoint = getMockEndpoint("mock:create-sale-order-line");
        mockCreateSaleOrderLineEndpoint.expectedMessageCount(1);
        mockCreateSaleOrderLineEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "c");
        mockCreateSaleOrderLineEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_SALE_ORDER_LINE_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(createHeaders);
            exchange.getMessage().setBody(saleOrderLine);
        });

        // Verify
        mockCreateSaleOrderLineEndpoint.assertIsSatisfied();
    }
}
