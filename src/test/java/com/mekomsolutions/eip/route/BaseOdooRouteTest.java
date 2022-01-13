package com.mekomsolutions.eip.route;

import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_URI_ERROR_HANDLER;

import org.apache.camel.Exchange;
import org.junit.Before;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.Constants;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.TestConstants;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

@Import(TestConfig.class)
@TestPropertySource(properties = PROP_URI_ERROR_HANDLER + "=" + TestConstants.URI_TEST_ERROR_HANDLER)
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = Constants.OPENMRS_DATASOURCE_NAME, transactionManager = "openmrsTestTxManager"))
public abstract class BaseOdooRouteTest extends BaseWatcherRouteTest {
	
	protected final ObjectMapper mapper = new ObjectMapper();
	
	protected String getErrorMessage(Exchange e) {
		return e.getProperty("error", EIPException.class).getMessage();
	}
	
	@Before
	public void setupBaseOdooRouteTest() {
		AppContext.remove(OdooTestConstants.ODOO_USER_ID_KEY);
	}
	
}
