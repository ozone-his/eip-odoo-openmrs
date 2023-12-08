package com.ozonehis.eip.route.prp;

import static com.ozonehis.eip.route.OdooTestConstants.APP_PROP_NAME_OBS_TO_ODOO_HANDLER;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_ADMISSION_EVENT;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_CUSTOMER;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_DISCHARGE_EVENT;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_INVOICE_EVENT;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_OBS_TO_RES_HANDLER;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_ADMISSION_EVENT;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_CUSTOMER;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_DISCHARGE_EVENT;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_INVOICE_EVENT;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_TO_RES_HANDLER;

import org.apache.camel.EndpointInject;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = APP_PROP_NAME_OBS_TO_ODOO_HANDLER + "=" + ROUTE_ID_OBS_TO_RES_HANDLER)
public class ObsToOdooResourceHandlerRouteTest extends BasePrpRouteTest {

    @EndpointInject("mock:" + ROUTE_ID_OBS_TO_CUSTOMER)
    private MockEndpoint mockObsToCustomerEndpoint;

    @EndpointInject("mock:" + ROUTE_ID_OBS_TO_ADMISSION_EVENT)
    private MockEndpoint mockAdmissionEndpoint;

    @EndpointInject("mock:" + ROUTE_ID_OBS_TO_DISCHARGE_EVENT)
    private MockEndpoint mockDischargeEndpoint;

    @EndpointInject("mock:" + ROUTE_ID_OBS_TO_INVOICE_EVENT)
    private MockEndpoint mockInvoiceEndpoint;

    @Before
    public void setup() throws Exception {
        mockObsToCustomerEndpoint.reset();
        mockAdmissionEndpoint.reset();
        mockDischargeEndpoint.reset();

        advise(ROUTE_ID_OBS_TO_RES_HANDLER, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                interceptSendToEndpoint(URI_OBS_TO_CUSTOMER)
                        .skipSendToOriginalEndpoint()
                        .to(mockObsToCustomerEndpoint);
                interceptSendToEndpoint(URI_OBS_TO_ADMISSION_EVENT)
                        .skipSendToOriginalEndpoint()
                        .to(mockAdmissionEndpoint);
                interceptSendToEndpoint(URI_OBS_TO_DISCHARGE_EVENT)
                        .skipSendToOriginalEndpoint()
                        .to(mockDischargeEndpoint);
                interceptSendToEndpoint(URI_OBS_TO_INVOICE_EVENT)
                        .skipSendToOriginalEndpoint()
                        .to(mockInvoiceEndpoint);
            }
        });
    }

    @Test
    public void shouldCallTheAppropriateRoutes() throws Exception {
        mockObsToCustomerEndpoint.expectedMessageCount(1);
        mockAdmissionEndpoint.expectedMessageCount(1);
        mockDischargeEndpoint.expectedMessageCount(1);
        mockInvoiceEndpoint.expectedMessageCount(1);

        producerTemplate.send(URI_OBS_TO_RES_HANDLER, new DefaultExchange(camelContext));

        mockObsToCustomerEndpoint.assertIsSatisfied();
        mockAdmissionEndpoint.assertIsSatisfied();
        mockDischargeEndpoint.assertIsSatisfied();
        mockInvoiceEndpoint.assertIsSatisfied();
    }
}
