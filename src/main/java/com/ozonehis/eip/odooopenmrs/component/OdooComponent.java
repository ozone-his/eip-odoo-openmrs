package com.ozonehis.eip.odooopenmrs.component;

import com.ozonehis.eip.odooopenmrs.client.OdooClient;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.openmrs.eip.mysql.watcher.MySqlWatcherComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Component("odoo")
public class OdooComponent extends DefaultComponent {

    @Autowired
    private OdooClient odooClient;

    private static final Logger logger = LoggerFactory.getLogger(MySqlWatcherComponent.class);

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        logger.info("Creating odoo endpoint with uri: {} remaining: {} parameters: {} ", uri, remaining, parameters);
        String[] parts = remaining.split("/", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid URI format. The expected format is 'odoo:method/model'");
        }
        return new OdooEndpoint(uri, this, parts[0], parts[1], odooClient);
    }
}
