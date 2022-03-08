package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.GetConceptByMappingFromOpenmrsRouteTest.EX_PROP_CODE;
import static com.mekomsolutions.eip.route.GetConceptByMappingFromOpenmrsRouteTest.EX_PROP_SOURCE;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_CONVERT_TO_CONCEPT_UUID;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_CONCEPT_BY_MAPPING;
import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConvertToConceptUuidIfIsMappingRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "convert-to-concept-uuid-if-is-mapping";
	
	@EndpointInject("mock:get-concept-by-mapping-from-openmrs")
	private MockEndpoint mockGetConceptByMapEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockGetConceptByMapEndpoint.reset();
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint(URI_GET_CONCEPT_BY_MAPPING).skipSendToOriginalEndpoint()
				        .to(mockGetConceptByMapEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldConvertAConceptMappingToAUuid() throws Exception {
		final String source = "test-source";
		final String code = "test-code";
		final String expectedConceptUuid = "test-uuid";
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(source + ":" + code);
		mockGetConceptByMapEndpoint.expectedMessageCount(1);
		mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_SOURCE, source);
		mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_CODE, code);
		mockGetConceptByMapEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody(Collections.singletonMap("uuid", expectedConceptUuid));
		});
		
		producerTemplate.send(URI_CONVERT_TO_CONCEPT_UUID, exchange);
		
		mockGetConceptByMapEndpoint.assertIsSatisfied();
		assertEquals(expectedConceptUuid, exchange.getIn().getBody(String.class));
	}
	
	@Test
	public void shouldReturnNullIfNoConceptIsFoundMatchingTheMapping() throws Exception {
		final String source = "test-source";
		final String code = "test-code";
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(source + ":" + code);
		mockGetConceptByMapEndpoint.expectedMessageCount(1);
		mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_SOURCE, source);
		mockGetConceptByMapEndpoint.expectedPropertyReceived(EX_PROP_CODE, code);
		mockGetConceptByMapEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody(null);
		});
		
		producerTemplate.send(URI_CONVERT_TO_CONCEPT_UUID, exchange);
		
		mockGetConceptByMapEndpoint.assertIsSatisfied();
		Assert.assertNull(exchange.getIn().getBody());
	}
	
	@Test
	public void shouldReturnTheOriginalValueIfItIsNotAValidConceptMapping() throws Exception {
		final String testConceptUuid = "test-uuid";
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.getIn().setBody(testConceptUuid);
		mockGetConceptByMapEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_CONVERT_TO_CONCEPT_UUID, exchange);
		
		mockGetConceptByMapEndpoint.assertIsSatisfied();
		assertEquals(testConceptUuid, exchange.getIn().getBody(String.class));
	}
	
}