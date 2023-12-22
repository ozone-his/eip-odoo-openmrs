package com.ozonehis.eip.odooopenmrs.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.BeforeEach;
import org.openmrs.eip.AppContext;
import org.openmrs.eip.Constants;
import org.openmrs.eip.EIPException;
import org.openmrs.eip.mysql.watcher.route.BaseWatcherRouteTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

@ActiveProfiles("test")
@Import({ TestConfig.class })
@TestPropertySource(properties = { "camel.springboot.routes-collector-enabled=false" })
@Sql(
        value = {"classpath:sql/test_data.sql"},
        config = @SqlConfig(dataSource = Constants.OPENMRS_DATASOURCE_NAME, transactionManager = "openmrsTestTxManager"))
public abstract class BaseOdooRouteTest extends BaseWatcherRouteTest {

    protected final ObjectMapper mapper = new ObjectMapper();
    
    static {
       container.executeLiquibase("liquibase/liquibase-openmrs.xml");
    }

    @Override
    protected String getErrorMessage(Exchange e) {
        return e.getProperty("error", EIPException.class).getMessage();
    }

    @BeforeEach
    public void setupBaseOdooRouteTest() {
        AppContext.remove(OdooTestConstants.ODOO_USER_ID_KEY);
    }
}
