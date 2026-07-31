/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.odoo;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class PartnerHandler {

    private static final String PERSON_ATTRIBUTE_URL = "http://fhir.openmrs.org/ext/person-attribute";
    private static final String PERSON_ATTRIBUTE_TYPE_URL = "http://fhir.openmrs.org/ext/person-attribute-type";
    private static final String PERSON_ATTRIBUTE_VALUE_URL = "http://fhir.openmrs.org/ext/person-attribute-value";
    private static final String INSURANCE_COVERAGE_ATTRIBUTE_NAME = "Insurance Coverage Tier";

    @Value("${odoo.customer.dob.field}")
    private String odooCustomerDobField;

    @Value("${odoo.customer.id.field}")
    private String odooCustomerIdField;

    @Value("${insurance.coverage.tier.mode:required}")
    private String insuranceCoverageTierMode;

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private OdooUtils odooUtils;

    public List<String> partnerDefaultAttributes;

    public Partner getPartnerByID(String partnerRefID) {
        partnerDefaultAttributes = asList(
                "id",
                "name",
                "ref",
                "street",
                "street2",
                "city",
                "zip",
                "active",
                "comment",
                odooCustomerDobField,
                odooCustomerIdField,
                "property_product_pricelist");
        Object[] records = odooClient.searchAndRead(
                Constants.PARTNER_MODEL, List.of(asList("ref", "=", partnerRefID)), partnerDefaultAttributes);
        if (records == null) {
            throw new EIPException(
                    String.format("Got null response while searching for Partner with reference id %s", partnerRefID));
        } else if (records.length == 1) {
            log.debug("Partner exists with reference id {} record {}", partnerRefID, records[0]);
            return odooUtils.convertToObject((Map<String, Object>) records[0], Partner.class);
        } else if (records.length == 0) {
            log.warn("No Partner found with reference id {}", partnerRefID);
            return null;
        } else {
            log.warn("Multiple Partners exists with reference id {}", partnerRefID);
            throw new EIPException(String.format("Multiple Partners exists with reference id%s", partnerRefID));
        }
    }

    public Partner createOrUpdatePartner(ProducerTemplate producerTemplate, Patient patient, Integer companyId) {
        Partner fetchedPartner = getPartnerByID(patient.getIdPart());
        if (fetchedPartner != null && fetchedPartner.getPartnerId() > 0) {
            int partnerId = fetchedPartner.getPartnerId();
            log.info("Partner with reference id {} already exists, updating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            applyExplicitPricelist(patient, partner);
            partner.setPartnerId(partnerId);
            partner.setPartnerCompanyId(companyId);
            sendPartner(producerTemplate, "direct:odoo-update-partner-route", partner);
            return getPartnerByID(partner.getPartnerRef());
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            applyExplicitPricelist(patient, partner);
            partner.setPartnerCompanyId(companyId);
            sendPartner(producerTemplate, "direct:odoo-create-partner-route", partner);
            return getPartnerByID(partner.getPartnerRef());
        }
    }

    private void applyExplicitPricelist(Patient patient, Partner partner) {
        String tier = extractCoverageTier(patient);
        if (tier == null || tier.isBlank()) {
            if ("optional".equalsIgnoreCase(insuranceCoverageTierMode)) {
                return;
            }
            throw new EIPException(String.format("Patient %s is missing required %s person attribute", patient.getIdPart(), INSURANCE_COVERAGE_ATTRIBUTE_NAME));
        }
        String pricelistName = switch (tier.trim()) {
            case "50", "50%", "Insurance 50%" -> "Insurance 50%";
            case "60", "60%", "Insurance 60%" -> "Insurance 60%";
            case "70", "70%", "Insurance 70%" -> "Insurance 70%";
            case "80", "80%", "Insurance 80%" -> "Insurance 80%";
            case "90", "90%", "Insurance 90%" -> "Insurance 90%";
            case "100", "100%", "Insurance 100%" -> "Insurance 100%";
            default -> throw new EIPException(String.format("Unsupported %s value %s for patient %s", INSURANCE_COVERAGE_ATTRIBUTE_NAME, tier, patient.getIdPart()));
        };
        Object[] records = odooClient.searchAndRead(
                "product.pricelist", List.of(asList("name", "=", pricelistName)), List.of("id", "name"));
        if (records == null || records.length != 1) {
            throw new EIPException(String.format("Expected exactly one Odoo pricelist named %s for patient %s", pricelistName, patient.getIdPart()));
        }
        Object id = ((Map<String, Object>) records[0]).get("id");
        if (id == null) {
            throw new EIPException(String.format("Odoo pricelist %s has no id for patient %s", pricelistName, patient.getIdPart()));
        }
        partner.setPartnerPricelistId(Integer.parseInt(id.toString()));
    }

    private String extractCoverageTier(Patient patient) {
        for (Extension extension : patient.getExtension()) {
            if (!PERSON_ATTRIBUTE_URL.equals(extension.getUrl())) {
                continue;
            }
            String attributeName = null;
            Type attributeValue = null;
            for (Extension nested : extension.getExtension()) {
                if (PERSON_ATTRIBUTE_TYPE_URL.equals(nested.getUrl()) && nested.getValue() != null) {
                    attributeName = nested.getValue().primitiveValue();
                } else if (PERSON_ATTRIBUTE_VALUE_URL.equals(nested.getUrl())) {
                    attributeValue = nested.getValue();
                }
            }
            if (INSURANCE_COVERAGE_ATTRIBUTE_NAME.equals(attributeName)) {
                if (attributeValue instanceof CodeableConcept codeableConcept) {
                    if (codeableConcept.hasText()) return codeableConcept.getText();
                    if (!codeableConcept.getCoding().isEmpty()) return codeableConcept.getCodingFirstRep().getCode();
                }
                return attributeValue == null ? null : attributeValue.primitiveValue();
            }
        }
        return null;
    }

    public void sendPartner(ProducerTemplate producerTemplate, String endpointUri, Partner partner) {
        Map<String, Object> headers = new HashMap<>();
        if (endpointUri.contains("update")) {
            headers.put(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(partner.getPartnerId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, partner, headers);
    }
}
