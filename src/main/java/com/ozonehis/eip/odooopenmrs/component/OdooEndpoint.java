package com.ozonehis.eip.odooopenmrs.component;

import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import org.apache.camel.*;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Odoo component to integrate with Odoo XML RPC.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "odoo", title = "Odoo", syntax = "odoo:method/model", producerOnly = true)
public class OdooEndpoint extends DefaultEndpoint {

    @UriPath(description = "Odoo method name Eg. write")
    @Metadata(required = true)
    private String method;

    @UriPath(description = "Odoo model name Eg. res.partner")
    @Metadata(required = true)
    private String model;

    private static final Logger log = LoggerFactory.getLogger(OdooEndpoint.class);

    private OdooClient odooClient;

    public OdooEndpoint(String endpointUri, Component component, String method, String model, OdooClient odooClient) {
        super(endpointUri, component);
        this.method = method;
        this.model = model;
        this.odooClient = odooClient;
    }

    @Override
    public String getEndpointBaseUri() {
        return super.getEndpointBaseUri();
    }

    @Override
    public Producer createProducer() {
        log.info("Creating odoo producer");
        return new OdooProducer(this, odooClient);
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
