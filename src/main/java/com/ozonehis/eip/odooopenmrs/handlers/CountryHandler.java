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
public class CountryHandler {

    @Autowired
    private OdooClient odooClient;

    public Integer getCountryId(String countryName) {
        try {
            Object[] records = odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName));
            if (records.length > 1) {
                throw new EIPException(
                        String.format("Found %s countries in odoo matching name: %s", records.length, countryName));
            } else if (records.length == 0) {
                log.warn("No country found in odoo matching name: {}", countryName);
                return null;
            }
            return (Integer) records[0];
        } catch (XmlRpcException | MalformedURLException e) {
            throw new RuntimeException(
                    String.format("Error occurred while fetching country from Odoo %s", e.getMessage()), e);
        }
    }
}
