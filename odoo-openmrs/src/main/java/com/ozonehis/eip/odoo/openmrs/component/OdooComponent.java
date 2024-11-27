/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.component;

import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Setter
@Component("odoo")
public class OdooComponent extends DefaultComponent {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private OdooUtils odooUtils;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) {
        log.info("Creating odoo endpoint with uri: {} remaining: {} parameters: {} ", uri, remaining, parameters);
        String[] parts = remaining.split("/", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid URI format. The expected format is 'odoo:method/model'");
        }
        return new OdooEndpoint(uri, this, parts[0], parts[1], odooClient, odooUtils);
    }
}
