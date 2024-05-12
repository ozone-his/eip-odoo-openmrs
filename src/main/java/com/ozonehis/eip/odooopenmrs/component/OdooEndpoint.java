package com.ozonehis.eip.odooopenmrs.component;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Odoo component to integrate with Odoo XML RPC.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "odoo", title = "Odoo", syntax = "odoo:apiName/methodName",
        apiSyntax = "apiName/methodName", category = {Category.API})
public class OdooEndpoint extends DefaultEndpoint {

    @Override
    public String getEndpointBaseUri() {
        return super.getEndpointBaseUri();
    }

    @Override
    public Producer createProducer() throws Exception {
        return new OdooProducer(this);
    }

    @Override
    public boolean isSingletonProducer() {
        return super.isSingletonProducer();
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new RuntimeException("Consumer not implemented");
    }
}
