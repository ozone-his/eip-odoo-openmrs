package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
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

    public Partner partnerExists(String partnerRefID) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.PARTNER_MODEL,
                    List.of(asList("ref", "=", partnerRefID)),
                    Constants.partnerDefaultAttributes);
            if (records == null) {
                throw new EIPException(String.format(
                        "Got null response while searching for Partner with reference id %s", partnerRefID));
            } else if (records.length == 1) {
                log.info("Partner exists with reference id {} record {}", partnerRefID, records[0]);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Partner.class);
            } else if (records.length == 0) {
                log.info("No Partner found with reference id {}", partnerRefID);
                return null;
            } else {
                log.info("Multiple Partners exists with reference id {}", partnerRefID);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Partner.class);
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
        Partner fetchedPartner = partnerExists(patient.getIdPart());
        if (fetchedPartner != null && fetchedPartner.getPartnerId() > 0) {
            int partnerId = fetchedPartner.getPartnerId();
            log.info("Partner with reference id {} already exists, updating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            // TODO: Set Partner as inactive if Patient has decreased
            //            if (patient.hasDeceased()) {
            //                partner.setPartnerActive(false);
            //            }
            Map<String, Object> partnerHeaders = new HashMap<>();
            partnerHeaders.put(Constants.HEADER_ODOO_ATTRIBUTE_NAME, "id");
            partnerHeaders.put(Constants.HEADER_ODOO_ATTRIBUTE_VALUE, List.of(partnerId));
            producerTemplate.sendBodyAndHeaders("direct:odoo-update-partner-route", partner, partnerHeaders);
            return partnerId;
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            Map<String, Object> partnerHeaders = new HashMap<>();
            producerTemplate.sendBodyAndHeaders("direct:odoo-create-partner-route", partner, partnerHeaders);
            return partnerExists(partner.getPartnerRef()).getPartnerId();
        }
    }

    public void sendPartner(ProducerTemplate producerTemplate, String endpointUri, Partner partner) {
        Map<String, Object> headers = new HashMap<>();
        if (endpointUri.contains("update")) {
            headers.put(Constants.HEADER_ODOO_ATTRIBUTE_NAME, "id");
            headers.put(Constants.HEADER_ODOO_ATTRIBUTE_VALUE, List.of(partner.getPartnerId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, partner, headers);
    }
}
