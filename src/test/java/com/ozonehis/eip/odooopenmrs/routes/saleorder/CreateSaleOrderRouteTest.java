package com.ozonehis.eip.odooopenmrs.routes.saleorder;

import static org.apache.camel.builder.AdviceWith.adviceWith;

import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
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
public class CreateSaleOrderRouteTest extends CamelSpringTestSupport {

    private static final String CREATE_SALE_ORDER_ROUTE = "direct:odoo-create-sale-order-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new CreateSaleOrderRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("odoo-create-sale-order-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("odoo://create/sale.order").replace().to("mock:create-sale-order");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_SALE_ORDER_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateSaleOrder() throws Exception {
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderClientOrderRef("12345");
        saleOrder.setOrderState("draft");

        Map<String, Object> createHeaders = new HashMap<>();
        createHeaders.put(Constants.HEADER_FHIR_EVENT_TYPE, "c");

        // Expectations
        MockEndpoint mockCreateSaleOrderEndpoint = getMockEndpoint("mock:create-sale-order");
        mockCreateSaleOrderEndpoint.expectedMessageCount(1);
        mockCreateSaleOrderEndpoint.expectedHeaderReceived(Constants.HEADER_FHIR_EVENT_TYPE, "c");
        mockCreateSaleOrderEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_SALE_ORDER_ROUTE, exchange -> {
            exchange.getMessage().setHeaders(createHeaders);
            exchange.getMessage().setBody(saleOrder);
        });

        // Verify
        mockCreateSaleOrderEndpoint.assertIsSatisfied();
    }
}
