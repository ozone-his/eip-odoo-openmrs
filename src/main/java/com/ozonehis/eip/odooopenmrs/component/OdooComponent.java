package com.ozonehis.eip.odooopenmrs.component;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

@Component("odoo")
public class OdooComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        OdooEndpoint odooEndpoint = new OdooEndpoint();
        return odooEndpoint;
    }
}
