package com.ozonehis.eip.odooopenmrs.handlers;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Setter
@Component
public class PartnerHandler {

    @Autowired
    private OdooClient odooClient;

    /* odoo-get-customer.xml */
    public boolean partnerExists(String partnerRefID) {
        List<List<List<Object>>> searchQuery = Collections.singletonList(
                Collections.singletonList(Arrays.asList("ref", "=", partnerRefID)));
        try {
            Object[] records = (Object[]) odooClient.execute(Constants.SEARCH_METHOD, Constants.PARTNER_MODEL, searchQuery, null);
            if ((records != null) && (records.length > 0)) {
                return true;
            }
        } catch (XmlRpcException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}

