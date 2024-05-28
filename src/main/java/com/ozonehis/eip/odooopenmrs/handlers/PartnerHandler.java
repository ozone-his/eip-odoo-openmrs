package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class PartnerHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private PartnerMapper partnerMapper;

    public int partnerExists(String partnerRefID) {
        try {
            Object[] records = odooClient.search(Constants.PARTNER_MODEL, asList("ref", "=", partnerRefID));
            if (records == null) {
                throw new EIPException(String.format(
                        "Got null response while searching for Partner with reference id %s", partnerRefID));
            } else if (records.length == 1) {
                log.info("Partner exists with reference id {} record {}", partnerRefID, records[0]);
                return (Integer) records[0];
            } else if (records.length == 0) {
                log.info("No Partner found with reference id {}", partnerRefID);
                return 0;
            } else {
                log.info("Multiple Partners exists with reference id {}", partnerRefID);
                return (Integer) records[0];
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error(
                    "Error occurred while checking if partner exists with reference id {} error {}",
                    partnerRefID,
                    e.getMessage(),
                    e);
            throw new CamelExecutionException("Error occurred while checking if partner exists", null, e);
        }
    }

    public int ensurePartnerExistsAndUpdate(ProducerTemplate producerTemplate, Patient patient) {
        int partnerId = partnerExists(patient.getIdPart());
        if (partnerId > 0) {
            log.info("Partner with reference id {} already exists, updating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            Map<String, Object> partnerHeaders = new HashMap<>();
            partnerHeaders.put(Constants.HEADER_ODOO_ATTRIBUTE_NAME, "ref");
            partnerHeaders.put(Constants.HEADER_ODOO_ATTRIBUTE_VALUE, partner.getPartnerRef());
            producerTemplate.sendBodyAndHeaders("direct:odoo-update-partner-route", partner, partnerHeaders);
            return partnerId;
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            Map<String, Object> partnerHeaders = new HashMap<>();
            partnerHeaders.put(Constants.HEADER_ODOO_ATTRIBUTE_NAME, "ref");
            partnerHeaders.put(Constants.HEADER_ODOO_ATTRIBUTE_VALUE, partner.getPartnerRef());
            producerTemplate.sendBodyAndHeaders("direct:odoo-create-partner-route", partner, partnerHeaders);
            return partnerExists(partner.getPartnerRef());
        }
    }
}
