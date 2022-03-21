package com.mekomsolutions.eip.route;

import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_OP_CREATE;
import static com.mekomsolutions.eip.route.OdooTestConstants.ODOO_RPC_METHOD;
import static com.mekomsolutions.eip.route.OdooTestConstants.URI_SAVE_CALENDAR_EVENT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class SaveCalendarEventInOdooRouteTest extends BaseOdooApiRouteTest {
	
	private static final String MODEL_NAME = "calendar.event";
	
	public static final String EX_PROP_SUBJECT = "subject";
	
	public static final String EX_PROP_DESCRIPTION = "description";
	
	public static final String EX_PROP_ATTENDEE_PARTNER_IDS = "attendeePartnerIds";
	
	public static final String EX_PROP_START = "startDateTime";
	
	public static final String EX_PROP_DURATION = "duration";
	
	@Test
	public void shouldCallCreateTheCalendarEventInOdoo() throws Exception {
		Exchange exchange = buildExchange();
		final String subject = "Test Subject name";
		final String description = "Test Description";
		final List<Integer> partnerIds = asList(3, 4);
		exchange.setProperty(EX_PROP_SUBJECT, subject);
		exchange.setProperty(EX_PROP_DESCRIPTION, description);
		exchange.setProperty(EX_PROP_ATTENDEE_PARTNER_IDS, partnerIds);
		LocalDateTime start = LocalDateTime.of(2022, 1, 1, 1, 0, 0, 0);
		exchange.setProperty(EX_PROP_START, start);
		exchange.setProperty(EX_PROP_DURATION, 120);
		final ArrayList rpcArgs = new ArrayList();
		rpcArgs.add(APP_PROP_ODOO_DB);
		rpcArgs.add(USER_ID);
		rpcArgs.add(APP_PROP_ODOO_PASS);
		rpcArgs.add(MODEL_NAME);
		rpcArgs.add(ODOO_OP_CREATE);
		Map eventData = new HashMap();
		eventData.put("name", subject);
		eventData.put("description", description);
		eventData.put("start", "2022-01-01 01:00:00");
		eventData.put("stop", "2022-01-01 03:00:00");
		eventData.put("partner_ids", singletonList(asList(6, 0, partnerIds)));
		rpcArgs.add(singletonList(eventData));
		final int expectedEventId = 21;
		Mockito.when(mockXmlRpcClient.execute(mockXmlRpcClientConfig, ODOO_RPC_METHOD, rpcArgs)).thenReturn(expectedEventId);
		
		producerTemplate.send(URI_SAVE_CALENDAR_EVENT, exchange);
		
		Assert.assertEquals(expectedEventId, exchange.getIn().getBody());
	}
	
}
