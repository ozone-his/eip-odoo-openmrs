package com.ozonehis.eip.route.prp;

import static com.ozonehis.eip.route.ObsCapturedOnFormRuleRouteTest.EX_PROP_FORM_UUID;
import static com.ozonehis.eip.route.ObsCapturedOnFormRuleRouteTest.EX_PROP_OBS;
import static com.ozonehis.eip.route.OdooTestConstants.URI_OBS_CAPTURED_ON_FORM;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IsInitDecisionAfterRegValidatedObsRule extends BasePrpRouteTest {
	
	private static final String ROUTE_ID = "is-init-decision-after-reg-validated-obs-rule";
	
	private static final String URI = "direct:" + ROUTE_ID;
	
	@EndpointInject("mock:obs-captured-on-form-rule")
	private MockEndpoint mockObsCapturedOnFormEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockObsCapturedOnFormEndpoint.reset();
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_OBS_CAPTURED_ON_FORM).skipSendToOriginalEndpoint()
				        .to(mockObsCapturedOnFormEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldCallObsCapturedOnFormRuleWithTheInitialDecisionAfterRegistrationForm() throws Exception {
		final String formUuid = "97c09137-5bf6-4afc-8073-ccde16bb2698";
		Exchange exchange = new DefaultExchange(camelContext);
		Map obsResource = new HashMap();
		exchange.getIn().setBody(obsResource);
		mockObsCapturedOnFormEndpoint.expectedMessageCount(1);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(EX_PROP_OBS, obsResource);
		mockObsCapturedOnFormEndpoint.expectedPropertyReceived(EX_PROP_FORM_UUID, formUuid);
		mockObsCapturedOnFormEndpoint.whenAnyExchangeReceived(e -> e.getIn().setBody(true));
		
		producerTemplate.send(URI, exchange);
		
		mockObsCapturedOnFormEndpoint.assertIsSatisfied();
		Assert.assertTrue(exchange.getIn().getBody(Boolean.class));
	}
	
}
