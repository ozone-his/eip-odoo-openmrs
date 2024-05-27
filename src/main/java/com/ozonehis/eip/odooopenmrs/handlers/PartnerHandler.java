package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import java.net.MalformedURLException;
import java.util.HashMap;
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
                throw new EIPException(String.format("Multiple Partners exists with reference id %s", partnerRefID));
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

    public void ensurePartnerExistsAndUpdate(ProducerTemplate producerTemplate, Patient patient) {
        if (partnerExists(patient.getIdPart()) > 0) {
            log.info("Partner with reference id {} already exists, updating...", patient.getIdPart());
            var headers = new HashMap<String, Object>();
            headers.put(HEADER_FHIR_EVENT_TYPE, "u");
            headers.put(Constants.HEADER_ENABLE_PATIENT_SYNC, true);
            producerTemplate.sendBodyAndHeaders("direct:patient-to-partner-router", patient, headers);
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            var headers = new HashMap<String, Object>();
            headers.put(HEADER_FHIR_EVENT_TYPE, "c");
            headers.put(Constants.HEADER_ENABLE_PATIENT_SYNC, true);
            producerTemplate.sendBodyAndHeaders("direct:patient-to-partner-router", patient, headers);
        }
    }
}
