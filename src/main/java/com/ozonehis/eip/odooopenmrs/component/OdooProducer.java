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

    private OdooClient odooClient;

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
        log.info("OdooProducer: Endpoint {}", getEndpoint());
        log.info("OdooProducer: Model: {}, Method {}", model, method);
        log.info("OdooProducer: OdooClient {}", odooClient);

        if (method.equalsIgnoreCase(Constants.CREATE_METHOD)) {
            Object[] records =
                    (Object[]) odooClient.execute(method, model, List.of(OdooUtils.convertObjectToMap(body)), null);
            log.info("OdooProducer: Created {} in Odoo with id: {}", body, records[0]);
        } else if (method.equalsIgnoreCase(Constants.WRITE_METHOD)) {
            Object[] records = odooClient.search(
                    Constants.PARTNER_MODEL,
                    asList("ref", "=", OdooUtils.convertObjectToMap(body).get("ref")));
            Boolean response = (Boolean)
                    odooClient.write(model, asList(asList((Integer) records[0]), OdooUtils.convertObjectToMap(body)));
            log.info("OdooProducer: Updated {} in Odoo with id: {}", body, records[0]);
            log.info(
                    "OdooProducer: Map: {} response {} search id {}",
                    OdooUtils.convertObjectToMap(body),
                    response,
                    records[0]);
        }

        log.info("OdooProducer: Body: {}", body);
    }
}
