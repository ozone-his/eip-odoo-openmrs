package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import java.net.MalformedURLException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class CountryStateHandler {

    @Autowired
    private OdooClient odooClient;

    public Integer getStateId(String stateName) {
        try {
            Object[] records = odooClient.search(Constants.COUNTRY_STATE_MODEL, asList("name", "=", stateName));
            if (records.length > 1) {
                throw new EIPException(
                        String.format("Found %s states in odoo matching name: %s", records.length, stateName));
            } else if (records.length == 0) {
                log.warn("No state found in odoo matching name: {}", stateName);
                return null;
            }
            return (Integer) records[0];
        } catch (XmlRpcException | MalformedURLException e) {
            throw new RuntimeException("Error occurred while fetching state from Odoo", e);
        }
    }
}
