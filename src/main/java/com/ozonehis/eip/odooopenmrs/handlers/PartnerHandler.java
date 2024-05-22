package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import java.net.MalformedURLException;
import java.util.HashMap;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.Patient;
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
            if ((records != null) && (records.length > 0)) {
                return (Integer) records[0];
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error("Error while checking if partner exists with id {} error {}", partnerRefID, e.getMessage(), e);
        }
        return 0;
    }

    public void ensurePartnerExistsAndUpdate(ProducerTemplate producerTemplate, Patient patient) {
        if (partnerExists(patient.getIdPart()) > 0) {
            log.info("Customer with UUID {} already exists, updating...", patient.getIdPart());
            var headers = new HashMap<String, Object>();
            headers.put(Constants.HEADER_ODOO_DOCTYPE, "Partner");
            headers.put(HEADER_FHIR_EVENT_TYPE, "u");
            //            headers.put(HEADER_ENABLE_PATIENT_SYNC, true);
            producerTemplate.sendBodyAndHeaders("direct:patient-to-partner-router", patient, headers);
        } else {
            log.info("Partner with UUID {} does not exist, creating...", patient.getIdPart());
            var headers = new HashMap<String, Object>();
            headers.put(Constants.HEADER_ODOO_DOCTYPE, "Customer");
            headers.put(HEADER_FHIR_EVENT_TYPE, "c");
            //            headers.put(HEADER_ENABLE_PATIENT_SYNC, true);
            producerTemplate.sendBodyAndHeaders("direct:patient-to-partner-router", patient, headers);
        }
    }
}
