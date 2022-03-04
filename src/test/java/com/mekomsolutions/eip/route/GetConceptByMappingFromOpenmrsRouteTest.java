package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.HEADER_AUTH;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_GET_CONCEPT_BY_MAPPING;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ToDynamicDefinition;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.AppContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = "openmrs.username=" + GetConceptByMappingFromOpenmrsRouteTest.OPENMRS_USER)
@TestPropertySource(properties = "openmrs.password=" + GetConceptByMappingFromOpenmrsRouteTest.OPENMRS_PASS)
public class GetConceptByMappingFromOpenmrsRouteTest extends BaseOdooRouteTest {
	
	private static final String ROUTE_ID = "get-concept-by-mapping-from-openmrs";
	
	private static final String MAP_KEY = ROUTE_ID + "-sourceAndCodeToConceptMapKey";
	
	private static final String EX_PROP_SOURCE = "conceptSource";
	
	private static final String EX_PROP_CODE = "conceptCode";
	
	protected static final String OPENMRS_USER = "test";
	
	protected static final String OPENMRS_PASS = "test";
	
	private String openmrsAuth;
	
	@EndpointInject("mock:http")
	private MockEndpoint mockHttpEndpoint;
	
	@Before
	public void setup() throws Exception {
		AppContext.remove(MAP_KEY);
		mockHttpEndpoint.reset();
		final String openmrsUser = env.getProperty("openmrs.username");
		final String openmrsPassword = env.getProperty("openmrs.password");
		openmrsAuth = "Basic " + Base64.getEncoder().encodeToString((openmrsUser + ":" + openmrsPassword).getBytes());
		
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				weaveByType(ToDynamicDefinition.class).replace().to(mockHttpEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldReturnTheCachedConceptIfItAlreadyExists() throws Exception {
		final String source = "CIEL";
		final String code = "12345";
		final Map expectedConcept = singletonMap("uuid", "some-concept-uuid");
		AppContext.add(MAP_KEY, singletonMap(source + ":" + code, expectedConcept));
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_SOURCE, source);
		exchange.setProperty(EX_PROP_CODE, code);
		mockHttpEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_GET_CONCEPT_BY_MAPPING, exchange);
		
		mockHttpEndpoint.assertIsSatisfied();
		assertEquals(expectedConcept, exchange.getIn().getBody(Map.class));
	}
	
	@Test
	public void shouldFetchTheConceptIfDoesNotExistInTheCache() throws Exception {
		final String source = "CIEL";
		final String code = "12345";
		final String expectedConceptUUid = "some-concept-uuid";
		final Map expectedConcept = singletonMap("uuid", expectedConceptUUid);
		AppContext.add(MAP_KEY, new HashMap());
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_SOURCE, source);
		exchange.setProperty(EX_PROP_CODE, code);
		mockHttpEndpoint.expectedMessageCount(1);
		mockHttpEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody("{\"results\":[{\"uuid\":\"" + expectedConceptUUid + "\"}]}");
		});
		
		producerTemplate.send(URI_GET_CONCEPT_BY_MAPPING, exchange);
		
		mockHttpEndpoint.assertIsSatisfied();
		assertEquals(expectedConcept, exchange.getIn().getBody(Map.class));
		assertEquals(openmrsAuth, exchange.getIn().getHeader(HEADER_AUTH, String.class).trim());
		assertEquals("GET", exchange.getIn().getHeader(Exchange.HTTP_METHOD));
		assertEquals("source=" + source + "&code=" + code, exchange.getIn().getHeader(Exchange.HTTP_RAW_QUERY));
		
	}
	
	@Test
	public void shouldFetchTheConceptIfDoesNotExistInTheCacheAndTheCacheIsNotYetInitiated() throws Exception {
		assertNull(AppContext.get(MAP_KEY));
		final String source = "CIEL";
		final String code = "12345";
		final String expectedConceptUUid = "some-concept-uuid";
		final Map expectedConcept = singletonMap("uuid", expectedConceptUUid);
		final Exchange exchange = new DefaultExchange(camelContext);
		exchange.setProperty(EX_PROP_SOURCE, source);
		exchange.setProperty(EX_PROP_CODE, code);
		mockHttpEndpoint.expectedMessageCount(1);
		mockHttpEndpoint.whenAnyExchangeReceived(e -> {
			e.getIn().setBody("{\"results\":[{\"uuid\":\"" + expectedConceptUUid + "\"}]}");
		});
		
		producerTemplate.send(URI_GET_CONCEPT_BY_MAPPING, exchange);
		
		mockHttpEndpoint.assertIsSatisfied();
		assertEquals(expectedConcept, exchange.getIn().getBody(Map.class));
		assertEquals(openmrsAuth, exchange.getIn().getHeader(HEADER_AUTH, String.class).trim());
		assertEquals("GET", exchange.getIn().getHeader(Exchange.HTTP_METHOD));
		assertEquals("source=" + source + "&code=" + code, exchange.getIn().getHeader(Exchange.HTTP_RAW_QUERY));
	}
	
}
