package com.ozonehis.eip.odooopenmrs.component;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.openmrs.eip.mysql.watcher.MySqlWatcherComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component("odoo")
public class OdooComponent extends DefaultComponent {

    private static final Logger logger = LoggerFactory.getLogger(MySqlWatcherComponent.class);


    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        logger.info("Creating odoo endpoint with parameters: {}", parameters);
        return new OdooEndpoint(uri, this);
    }
}
