package com.ozonehis.eip.odooopenmrs.component;

import org.apache.camel.*;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Odoo component to integrate with Odoo XML RPC.
 */
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "odoo",
        title = "Odoo",
        syntax = "odoo://method/model",
        producerOnly = true)
public class OdooEndpoint extends DefaultEndpoint {

    @UriParam(description = "Odoo method name Eg. write")
    @Metadata(required = true)
    private String method;

    @UriParam(description = "Odoo model name Eg. res.partner")
    @Metadata(required = true)
    private String model;

    private static final Logger log = LoggerFactory.getLogger(OdooEndpoint.class);

    public OdooEndpoint(String endpointUri, Component component) {
        this.setEndpointUri(endpointUri);
        this.setComponent(component);
    }

    @Override
    public String getEndpointBaseUri() {
        return super.getEndpointBaseUri();
    }

    @Override
    public Producer createProducer() {
        log.info("Creating odoo producer");
        return new OdooProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
