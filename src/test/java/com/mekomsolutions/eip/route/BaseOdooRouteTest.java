package com.mekomsolutions.eip.route;

import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_URI_ERROR_HANDLER;

import org.apache.camel.Exchange;
import org.openmrs.eip.TestConstants;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@TestPropertySource(properties = PROP_URI_ERROR_HANDLER + "=" + TestConstants.URI_TEST_ERROR_HANDLER)
@Sql(value = {
        "classpath:test_data.sql" }, config = @SqlConfig(dataSource = "openmrsDataSource", transactionManager = "openmrsTransactionManager"))
public abstract class BaseOdooRouteTest extends BaseWatcherRouteTest {
	
	protected String getErrorMessage(Exchange e) {
		return e.getProperty("error", EIPException.class).getMessage();
	}
	
}
