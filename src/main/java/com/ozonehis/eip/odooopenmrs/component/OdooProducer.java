package com.ozonehis.eip.odooopenmrs.component;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;

import java.util.List;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
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

        Object[] records = null;
        switch (method) {
            case Constants.CREATE_METHOD:
                log.info("OdooProducer: Creating data {} in Odoo", body);
                records = (Object[]) odooClient.create(method, model, List.of(OdooUtils.convertObjectToMap(body)), null);
                log.info("OdooProducer: Created data {} in Odoo with id: {}", body, records[0]);
                break;
            case Constants.WRITE_METHOD:
                log.info("OdooProducer: Updating data {} in Odoo", body);
                records = odooClient.search(
                        Constants.PARTNER_MODEL,
                        asList("ref", "=", OdooUtils.convertObjectToMap(body).get("ref"))); // TODO: Check if required and fix
                Boolean response = (Boolean)
                        odooClient.write(model, asList(asList((Integer) records[0]), OdooUtils.convertObjectToMap(body)));
                log.info("OdooProducer: Updated data {} in Odoo with search id {} response: {}", OdooUtils.convertObjectToMap(body), records[0], response);
                break;
            default:
                log.error("OdooProducer: Unimplemented method name in Odoo component {} with body {}", method, body);
                break;
        }
    }
}
