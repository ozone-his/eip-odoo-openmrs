package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_OP;
import static com.mekomsolutions.eip.route.OdooTestConstants.EX_PROP_ODOO_PATIENT_ID;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.PATIENT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_PATIENT_HANDLER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.mysql.watcher.Event;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.Level;

@TestPropertySource(properties = {"openmrs.identifier.type.uuid=" + OdooPatientHandlerSyncsAnyCreatedPatientEventRouteTest.ID_TYPE_UUID, "should.sync.any.patient.asCustomer=true"})
public class OdooPatientHandlerSyncsAnyCreatedPatientEventRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "odoo-patient-handler";
	
	public static final String EX_PROP_PATIENT = "patient";
	
	public static final String EX_PROP_CREATE_CUSTOMER = "createCustomerIfNotExist";
	
	public static final String EX_PROP_CUSTOM_DATA = "customPatientData";
	
	public static final String ID_TYPE_UUID = "8d79403a-c2cc-11de-8d13-0010c6dffd0f";
	
	public static final String ID_TYPE_ID_KEY = "odoo-patient-handler-idTypeId";
	
	@EndpointInject("mock:odoo-get-customer")
	private MockEndpoint mockGetCustomerEndpoint;
	
	@EndpointInject("mock:odoo-manage-customer")
	private MockEndpoint mockManageCustomerEndpoint;
	
	@EndpointInject("mock:odoo-get-quotations")
	private MockEndpoint mockGetQuotesEndpoint;
	
	@EndpointInject("mock:odoo-cancel-quotations")
	private MockEndpoint mockCancelQuotesEndpoint;
	
	@EndpointInject("mock:odoo-process-person-address")
	private MockEndpoint mockProcessAddressEndpoint;
	
	@EndpointInject("mock:odoo-callback-get-custom-customer-data")
	private MockEndpoint mockGetCustomDataEndpoint;
	
	@Before
	public void setup() throws Exception {
		AppContext.remove(ID_TYPE_ID_KEY);
		mockGetCustomerEndpoint.reset();
		mockManageCustomerEndpoint.reset();
		mockProcessAddressEndpoint.reset();
		mockGetQuotesEndpoint.reset();
		mockCancelQuotesEndpoint.reset();
		mockGetCustomDataEndpoint.reset();
		mockGetCustomerEndpoint.expectedMessageCount(1);
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-get-customer").skipSendToOriginalEndpoint().to(mockGetCustomerEndpoint);
				interceptSendToEndpoint("direct:odoo-callback-get-custom-customer-data").skipSendToOriginalEndpoint()
				        .to(mockGetCustomDataEndpoint);
				interceptSendToEndpoint("direct:odoo-manage-customer").skipSendToOriginalEndpoint()
				        .to(mockManageCustomerEndpoint);
				interceptSendToEndpoint("direct:odoo-process-person-address").skipSendToOriginalEndpoint()
				        .to(mockProcessAddressEndpoint);
				interceptSendToEndpoint("direct:odoo-get-quotations").skipSendToOriginalEndpoint().to(mockGetQuotesEndpoint);
				interceptSendToEndpoint("direct:odoo-cancel-quotations").skipSendToOriginalEndpoint()
				        .to(mockCancelQuotesEndpoint);
			}
			
		});
	}
	
	@After
	public void tearDown() throws Exception {
		mockGetCustomerEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldAddPatientToOdooIfTheyDoNotExistAddCreateCustomerPropIsSetToFalseGivenshouldSyncAnyPatientAsCustomerIsTrue() throws Exception {
		Event event = createEvent("orders", "1", "order-uuid", "c");
		final Exchange exchange = new DefaultExchange(camelContext);
		Map personResource = new HashMap();
		final String name = "Test User";
		personResource.put("display", name);
		Map patientResource = new HashMap();
		patientResource.put("uuid", PATIENT_UUID);
		patientResource.put("person", personResource);
		exchange.setProperty(EX_PROP_PATIENT, patientResource);
		exchange.setProperty(EX_PROP_CREATE_CUSTOMER, false);
		exchange.setProperty(PROP_EVENT, event);
		mockGetCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(new Integer[] {}));
		mockGetCustomDataEndpoint.expectedMessageCount(1);
		mockGetCustomDataEndpoint.expectedPropertyReceived(EX_PROP_CUSTOM_DATA, new HashMap());
		mockProcessAddressEndpoint.expectedMessageCount(0);
		mockManageCustomerEndpoint.expectedMessageCount(1);
		mockManageCustomerEndpoint.expectedPropertyReceived(EX_PROP_ODOO_OP, ODOO_OP_CREATE);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient-name", name);
		mockManageCustomerEndpoint.expectedPropertyReceived("patient", patientResource);
		mockManageCustomerEndpoint.expectedPropertyReceived("patientIdentifier", "12345");
		final int patientId = 12;
		mockManageCustomerEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(patientId));
		
		producerTemplate.send(URI_PATIENT_HANDLER, exchange);
		
		mockProcessAddressEndpoint.assertIsSatisfied();
		mockManageCustomerEndpoint.assertIsSatisfied();
		mockGetCustomDataEndpoint.assertIsSatisfied();
		assertEquals(patientId, exchange.getProperty(EX_PROP_ODOO_PATIENT_ID));
		assertFalse(exchange.getProperty("isPatientVoidedOrDeleted", Boolean.class));
		assertMessageLogged(Level.DEBUG, "Patient has no address");
	}
}
