/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.component;

import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
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

    @Getter
    @Setter
    @UriPath(description = "Odoo method name Eg. write, create")
    @Metadata(required = true)
    private String method;

    @Setter
    @Getter
    @UriPath(description = "Odoo model name Eg. res.partner, sale.order")
    @Metadata(required = true)
    private String model;

    private static final Logger log = LoggerFactory.getLogger(OdooEndpoint.class);

    private final OdooClient odooClient;

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
        log.error("Odoo camel component consumer not supported");
        throw new UnsupportedOperationException();
    }
}
