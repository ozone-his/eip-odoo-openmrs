package com.ozonehis.eip.odooopenmrs.handlers;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.model.Uom;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.xmlrpc.XmlRpcException;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.Map;

import static java.util.Arrays.asList;

@Slf4j
@Setter
@Component
public class UomHandler {

    @Autowired
    private OdooClient odooClient;

    public Uom getUom(String externalId) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.IR_MODEL,
                    asList(asList("model", "=", Constants.UOM_MODEL), asList("name", "=", externalId)),
                    null);
            if (records == null) {
                throw new EIPException(String.format("Got null response while fetching for Uom with id %s", externalId));
            } else if (records.length == 1) {
                log.info("Uom exists with id {} record {}", externalId, records[0]);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Uom.class);
            } else if (records.length == 0) {
                log.info("No Uom found with id {}", externalId);
                throw new EIPException(String.format("No Uom found with id %s", externalId));
            } else {
                throw new EIPException(String.format("Multiple Uom exists with id %s", externalId));
            }
        } catch (MalformedURLException | XmlRpcException e) {
            log.error("Error occurred while fetching uom with id {} error {}", externalId, e.getMessage(), e);
            throw new CamelExecutionException("Error occurred while fetching uom", null, e);
        }
    }
}
