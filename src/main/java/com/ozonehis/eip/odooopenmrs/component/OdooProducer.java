package com.ozonehis.eip.odooopenmrs.component;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
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

        switch (method) {
            case Constants.CREATE_METHOD:
                log.info("OdooProducer: Creating data {} in Odoo", body);
                Object record = odooClient.create(model, List.of(OdooUtils.convertObjectToMap(body)));
                if (record == null) {
                    throw new EIPException(
                            String.format("Got null response while creating %s with body %s", model, body));
                }
                log.info("OdooProducer: Created data {} in Odoo with id: {}", body, record);
                break;
            case Constants.WRITE_METHOD:
                log.info("OdooProducer: Updating data {} in Odoo", body);
                String attributeName =
                        exchange.getMessage().getHeader(Constants.HEADER_ODOO_ATTRIBUTE_NAME, String.class);
                String attributeValue =
                        exchange.getMessage().getHeader(Constants.HEADER_ODOO_ATTRIBUTE_VALUE, String.class);
                log.info("OdooProducer: Fetching {} model {} with value {}", model, attributeName, attributeValue);
                Object[] records = odooClient.search(
                        model,
                        asList(
                                attributeName,
                                "=",
                                OdooUtils.convertObjectToMap(body).get(attributeValue)));
                if (records.length > 1) {
                    throw new EIPException(String.format(
                            "Found %s %s in odoo with matching value: %s", records.length, model, attributeValue));
                } else if (records.length == 0) {
                    log.error("No {} found in odoo matching name: {}", model, attributeName);
                    throw new EIPException(
                            String.format("No %s found in odoo matching name: %s", model, attributeName));
                }

                log.info("OdooProducer: Writing {} model for id {}", model, records[0]);
                Boolean response = (Boolean) odooClient.write(
                        model, asList(asList((Integer) records[0]), OdooUtils.convertObjectToMap(body)));
                if (response == null) {
                    throw new EIPException(String.format(
                            "Got null response while updating %s with %s", model, OdooUtils.convertObjectToMap(body)));
                } else if (response) {
                    log.info("{} updated with body {}", model, OdooUtils.convertObjectToMap(body));
                } else {
                    throw new EIPException(
                            String.format("Unable to update %s with %s", model, OdooUtils.convertObjectToMap(body)));
                }
                break;
            default:
                log.error("OdooProducer: Unimplemented method name in Odoo component {} with body {}", method, body);
                break;
        }
    }
}
