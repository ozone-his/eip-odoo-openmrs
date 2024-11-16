/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.component;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.openmrs.eip.EIPException;

@Slf4j
public class OdooProducer extends DefaultProducer {

    private final OdooUtils odooUtils;

    private final OdooClient odooClient;

    public OdooProducer(Endpoint endpoint, OdooClient odooClient, OdooUtils odooUtils) {
        super(endpoint);
        this.odooClient = odooClient;
        this.odooUtils = odooUtils;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody(Object.class);

        String model = ((OdooEndpoint) getEndpoint()).getModel();
        String method = ((OdooEndpoint) getEndpoint()).getMethod();
        log.debug("OdooProducer: Endpoint {} Model: {}, Method {} ", getEndpoint(), model, method);

        Object attributeValue = exchange.getMessage().getHeader(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, Object.class);
        switch (method) {
            case Constants.CREATE_METHOD:
                create(model, body);
                break;
            case Constants.WRITE_METHOD:
                write(model, body, (List<Integer>) attributeValue);
                break;
            case Constants.UNLINK_METHOD:
                delete(model, body, (List<Integer>) attributeValue);
                break;
            default:
                log.error("OdooProducer: Unimplemented method name in Odoo component {} with body {}", method, body);
                break;
        }
    }

    private void create(String model, Object body) throws Exception {
        log.debug("OdooProducer: Creating data {} in Odoo", body);
        Integer record = odooClient.create(model, List.of(odooUtils.convertObjectToMap(body)));
        if (record == null) {
            throw new EIPException(String.format("Got null response while creating %s with body %s", model, body));
        }
        log.debug("OdooProducer: Created data {} in Odoo with id: {}", body, record);
    }

    private void write(String model, Object body, List<Integer> ids) throws Exception {
        log.debug("OdooProducer: Writing {} model for id {}", model, ids);
        Boolean response = odooClient.write(model, asList(ids, odooUtils.convertObjectToMap(body)));
        if (response == null) {
            throw new EIPException(String.format(
                    "Got null response while updating %s with %s", model, odooUtils.convertObjectToMap(body)));
        } else if (response) {
            log.debug("{} updated with body {}", model, odooUtils.convertObjectToMap(body));
        } else {
            throw new EIPException(
                    String.format("Unable to update %s with %s", model, odooUtils.convertObjectToMap(body)));
        }
    }

    private void delete(String model, Object body, List<Integer> ids) throws Exception {
        log.debug("OdooProducer: Deleting {} model for ids {}", model, ids);
        Boolean response = odooClient.delete(model, Collections.singletonList(ids));
        if (response == null) {
            throw new EIPException(String.format(
                    "Got null response while deleting %s with %s", model, odooUtils.convertObjectToMap(body)));
        } else if (response) {
            log.debug("{} deleted with body {}", model, odooUtils.convertObjectToMap(body));
        } else {
            throw new EIPException(
                    String.format("Unable to delete %s with %s", model, odooUtils.convertObjectToMap(body)));
        }
    }
}
