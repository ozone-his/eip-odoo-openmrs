package com.ozonehis.eip.odooopenmrs.component;

import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class OdooProducer extends DefaultProducer {

    @Autowired
    private OdooClient odooClient;

    private static final Logger log = LoggerFactory.getLogger(OdooProducer.class);

    public OdooProducer(Endpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);

        String model = ((OdooEndpoint) getEndpoint()).getModel();
        String method = ((OdooEndpoint) getEndpoint()).getMethod();

        // TODO: Parse params from body
        List<Object> params = new ArrayList<>();
        HashMap<String, Object> hashMap = new HashMap<>();
        params.add(hashMap);

        //        odooClient.execute(method, model, params, null);
        // TODO: Parse params and requestParams
        log.info("OdooProducer: Body: {}", body);
    }
}
