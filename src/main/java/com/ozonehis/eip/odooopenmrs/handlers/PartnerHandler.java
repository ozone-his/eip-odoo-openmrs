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
                log.debug("Partner exists with reference id {} record {}", partnerRefID, records[0]);
                return OdooUtils.convertToObject((Map<String, Object>) records[0], Partner.class);
            } else if (records.length == 0) {
                log.warn("No Partner found with reference id {}", partnerRefID);
                return null;
            } else {
                log.warn("Multiple Partners exists with reference id {}", partnerRefID);
                throw new EIPException(String.format("Multiple Partners exists with reference id%s", partnerRefID));
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
            partner.setPartnerId(partnerId);
            sendPartner(producerTemplate, "direct:odoo-update-partner-route", partner);
            return partnerId;
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            sendPartner(producerTemplate, "direct:odoo-create-partner-route", partner);
            return partnerExists(partner.getPartnerRef()).getPartnerId();
        }
    }

    public void sendPartner(ProducerTemplate producerTemplate, String endpointUri, Partner partner) {
        Map<String, Object> headers = new HashMap<>();
        if (endpointUri.contains("update")) {
            headers.put(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(partner.getPartnerId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, partner, headers);
    }
}
