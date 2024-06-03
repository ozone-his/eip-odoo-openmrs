package com.ozonehis.eip.odooopenmrs.component;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import java.util.Collections;
import java.util.List;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.openmrs.eip.EIPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OdooProducer extends DefaultProducer {

    private final OdooClient odooClient;

    private static final Logger log = LoggerFactory.getLogger(OdooProducer.class);

    public OdooProducer(Endpoint endpoint, OdooClient odooClient) {
        super(endpoint);
        this.odooClient = odooClient;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody(Object.class);

        String model = ((OdooEndpoint) getEndpoint()).getModel();
        String method = ((OdooEndpoint) getEndpoint()).getMethod();
        log.info("OdooProducer: Endpoint {} Model: {}, Method {} ", getEndpoint(), model, method);

        String attributeName = exchange.getMessage().getHeader(Constants.HEADER_ODOO_ATTRIBUTE_NAME, String.class);
        Object attributeValue = exchange.getMessage().getHeader(Constants.HEADER_ODOO_ATTRIBUTE_VALUE, Object.class);
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
        log.info("OdooProducer: Creating data {} in Odoo", body);
        Integer record = odooClient.create(model, List.of(OdooUtils.convertObjectToMap(body)));
        if (record == null) {
            throw new EIPException(String.format("Got null response while creating %s with body %s", model, body));
        }
        log.info("OdooProducer: Created data {} in Odoo with id: {}", body, record);
    }

    private void write(String model, Object body, List<Integer> ids) throws Exception {
        log.info("OdooProducer: Writing {} model for id {}", model, ids);
        Boolean response = odooClient.write(model, asList(ids, OdooUtils.convertObjectToMap(body)));
        if (response == null) {
            throw new EIPException(String.format(
                    "Got null response while updating %s with %s", model, OdooUtils.convertObjectToMap(body)));
        } else if (response) {
            log.info("{} updated with body {}", model, OdooUtils.convertObjectToMap(body));
        } else {
            throw new EIPException(
                    String.format("Unable to update %s with %s", model, OdooUtils.convertObjectToMap(body)));
        }
    }

    private void delete(String model, Object body, List<Integer> ids) throws Exception {
        log.info("OdooProducer: Deleting {} model for ids {}", model, ids);
        Boolean response = odooClient.delete(model, Collections.singletonList(ids));
        if (response == null) {
            throw new EIPException(String.format(
                    "Got null response while deleting %s with %s", model, OdooUtils.convertObjectToMap(body)));
        } else if (response) {
            log.info("{} deleted with body {}", model, OdooUtils.convertObjectToMap(body));
        } else {
            throw new EIPException(
                    String.format("Unable to delete %s with %s", model, OdooUtils.convertObjectToMap(body)));
        }
    }
}
