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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final String INSURANCE_PLAN_NAME_TEMPLATE = "Insurance %d%%";
    private static final String INSURANCE_PLAN_REF_PREFIX = "INS-";
    private static final String COVERAGE_MODEL = "insurance.product.coverage";
    private static final String PRODUCT_MODEL = "product.product";

    @Value("${odoo.customer.dob.field}")
    private String odooCustomerDobField;

    @Value("${odoo.customer.id.field}")
    private String odooCustomerIdField;

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
        Partner partner = partnerMapper.toOdoo(patient);
        applyAddonModelCoverage(patient, partner);
        if (fetchedPartner != null && fetchedPartner.getPartnerId() > 0) {
            log.info("Partner with reference id {} already exists, updating...", patient.getIdPart());
            partner.setPartnerId(fetchedPartner.getPartnerId());
            partner.setPartnerCompanyId(companyId);
            sendPartner(producerTemplate, "direct:odoo-update-partner-route", partner);
            return getPartnerByID(partner.getPartnerRef());
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            partner.setPartnerCompanyId(companyId);
            sendPartner(producerTemplate, "direct:odoo-create-partner-route", partner);
            return getPartnerByID(partner.getPartnerRef());
        }
    }

    /**
     * Fail-closed insurance coverage tier validation (issue #189): a patient
     * without the Insurance Coverage Tier person attribute, or with an
     * unsupported value, is rejected instead of silently falling back to a
     * default plan/pricelist. Pricing itself is the insurance_coverage addon's
     * job (issue #184): the addon splits full-priced invoices between the
     * payers, so the EIP deliberately does NOT assign a discount pricelist to
     * the partner (a discounted sale order would make the invoice carry the
     * patient share instead of the full service value, breaking the split).
     */
    public int validateCoverageTier(Patient patient) {
        String tier = extractCoverageTier(patient);
        if (tier == null || tier.isBlank()) {
            throw new EIPException(String.format(
                    "Patient %s is missing required %s person attribute",
                    patient.getIdPart(), INSURANCE_COVERAGE_ATTRIBUTE_NAME));
        }
        int percent =
                switch (tier.trim()) {
                    case "50", "50%", "Insurance 50%" -> 50;
                    case "60", "60%", "Insurance 60%" -> 60;
                    case "70", "70%", "Insurance 70%" -> 70;
                    case "80", "80%", "Insurance 80%" -> 80;
                    case "90", "90%", "Insurance 90%" -> 90;
                    case "100", "100%", "Insurance 100%" -> 100;
                    default -> throw new EIPException(String.format(
                            "Unsupported %s value %s for patient %s",
                            INSURANCE_COVERAGE_ATTRIBUTE_NAME, tier, patient.getIdPart()));
                };
        log.info("Patient {} has insurance coverage tier {}%", patient.getIdPart(), percent);
        return percent;
    }

    /**
     * Mirrors the insurance tier into the insurance_coverage addon model (issue #184):
     * find-or-create the base plan partner for the tier, enrol the patient in it and
     * make sure every saleable product has a coverage row for the plan
     * (coverage_percentage = tier, covered_base_mode = full). Complements the
     * pricelist mapping: the pricelist prices the sale order, the addon model splits
     * the invoice. Idempotent by plan ref and the (insurance_id, product_id) unique
     * constraint. Fails closed on missing/unsupported tiers.
     */
    public void applyAddonModelCoverage(Patient patient, Partner partner) {
        int percent = resolveTierPercent(patient);
        String planName = String.format(INSURANCE_PLAN_NAME_TEMPLATE, percent);
        String planRef = INSURANCE_PLAN_REF_PREFIX + percent;
        int planId = findOrCreateInsurancePlan(planName, planRef);
        partner.setPartnerBaseInsuranceId(planId);
        ensureCoverageRows(planId, percent);
    }

    private int resolveTierPercent(Patient patient) {
        String tier = extractCoverageTier(patient);
        if (tier == null || tier.isBlank()) {
            throw new EIPException(String.format(
                    "Patient %s is missing required %s person attribute",
                    patient.getIdPart(), INSURANCE_COVERAGE_ATTRIBUTE_NAME));
        }
        return switch (tier.trim()) {
            case "50", "50%", "Insurance 50%" -> 50;
            case "60", "60%", "Insurance 60%" -> 60;
            case "70", "70%", "Insurance 70%" -> 70;
            case "80", "80%", "Insurance 80%" -> 80;
            case "90", "90%", "Insurance 90%" -> 90;
            case "100", "100%", "Insurance 100%" -> 100;
            default -> throw new EIPException(String.format(
                    "Unsupported %s value %s for patient %s",
                    INSURANCE_COVERAGE_ATTRIBUTE_NAME, tier, patient.getIdPart()));
        };
    }

    private int findOrCreateInsurancePlan(String planName, String planRef) {
        Object[] records = odooClient.searchAndRead(
                Constants.PARTNER_MODEL, List.of(asList("ref", "=", planRef)), List.of("id", "name"));
        if (records != null && records.length > 1) {
            throw new EIPException(String.format("Multiple Odoo insurance plan partners exist with ref %s", planRef));
        }
        if (records != null && records.length == 1) {
            Object id = ((Map<String, Object>) records[0]).get("id");
            if (id == null) {
                throw new EIPException(String.format("Odoo insurance plan partner %s has no id", planRef));
            }
            return Integer.parseInt(id.toString());
        }
        Integer planId = odooClient.create(
                Constants.PARTNER_MODEL,
                List.of(Map.of("name", planName, "ref", planRef, "is_insurance", true, "insurance_type", "base")));
        if (planId == null || planId <= 0) {
            throw new EIPException(
                    String.format("Failed to create Odoo insurance plan partner %s (%s)", planName, planRef));
        }
        return planId;
    }

    private void ensureCoverageRows(int planId, int coveragePercentage) {
        Object[] products = odooClient.searchAndRead(
                PRODUCT_MODEL, List.of(asList("active", "=", true), asList("sale_ok", "=", true)), List.of("id"));
        if (products == null || products.length == 0) {
            return;
        }
        Object[] existing = odooClient.searchAndRead(
                COVERAGE_MODEL, List.of(asList("insurance_id", "=", planId)), List.of("product_id"));
        Set<Integer> coveredProductIds = new HashSet<>();
        if (existing != null) {
            for (Object record : existing) {
                Object productId = ((Map<String, Object>) record).get("product_id");
                if (productId != null) {
                    coveredProductIds.add(asId(productId));
                }
            }
        }
        List<Map<String, Object>> coverageRows = new ArrayList<>();
        for (Object productRecord : products) {
            Object productId = ((Map<String, Object>) productRecord).get("id");
            if (productId == null) {
                continue;
            }
            int pid = asId(productId);
            if (coveredProductIds.contains(pid)) {
                continue;
            }
            coverageRows.add(Map.of(
                    "insurance_id",
                    planId,
                    "product_id",
                    pid,
                    "coverage_percentage",
                    (double) coveragePercentage,
                    "covered_base_mode",
                    "full"));
        }
        for (Map<String, Object> row : coverageRows) {
            Integer created = odooClient.create(COVERAGE_MODEL, List.of(row));
            if (created == null || created <= 0) {
                throw new EIPException(String.format("Failed to create coverage row for insurance plan %d", planId));
            }
        }
    }

    private int asId(Object value) {
        if (value instanceof Object[] array) {
            return Integer.parseInt(array[0].toString());
        }
        return Integer.parseInt(value.toString());
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
                    if (!codeableConcept.getCoding().isEmpty())
                        return codeableConcept.getCodingFirstRep().getCode();
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
