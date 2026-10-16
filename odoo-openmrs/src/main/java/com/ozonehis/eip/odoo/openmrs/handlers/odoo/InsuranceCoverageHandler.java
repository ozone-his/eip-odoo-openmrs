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
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Opt-in insurance coverage integration (issues #184/#189).
 *
 * <p>Disabled by default: unless {@code odoo.insurance.enabled} is set to {@code true}, this
 * handler does not change the behavior of any other feature. When enabled, the insurance coverage
 * tier is read from the configured OpenMRS person attribute and validated fail-closed against the
 * configured tier set: a patient without the attribute, or with an unsupported value, is rejected
 * instead of silently falling back to a default plan.
 *
 * <p>Pricing itself is the insurance_coverage addon's job (issue #184): the addon splits full-priced
 * invoices between the payers, so the EIP deliberately does NOT assign a discount pricelist to the
 * partner (a discounted sale order would make the invoice carry the patient share instead of the
 * full service value, breaking the split).
 */
@Slf4j
@Setter
@Component
public class InsuranceCoverageHandler {

    private static final String PERSON_ATTRIBUTE_URL = "http://fhir.openmrs.org/ext/person-attribute";
    private static final String PERSON_ATTRIBUTE_TYPE_URL = "http://fhir.openmrs.org/ext/person-attribute-type";
    private static final String PERSON_ATTRIBUTE_VALUE_URL = "http://fhir.openmrs.org/ext/person-attribute-value";

    private static final String MISSING_TIER_POLICY_SKIP = "skip";

    /** Returned by {@link #validateCoverageTier(Patient)} when the tier is missing and the missing-tier policy is {@code skip}. */
    private static final int NO_TIER = -1;

    private static final String PRODUCT_MODEL = "product.product";

    @Value("${odoo.insurance.enabled:false}")
    private boolean insuranceEnabled;

    @Value("${odoo.insurance.attribute.name:Insurance Coverage Tier}")
    private String insuranceAttributeName;

    @Value("${odoo.insurance.tiers:50,60,70,80,90,100}")
    private String configuredTiers;

    @Value("${odoo.insurance.plan.name.template:Insurance %d%%}")
    private String planNameTemplate;

    @Value("${odoo.insurance.missing.tier.policy:reject}")
    private String missingTierPolicy;

    @Value("${odoo.insurance.addon.enabled:false}")
    private boolean addonEnabled;

    @Value("${odoo.insurance.plan.ref.prefix:INS-}")
    private String planRefPrefix;

    @Value("${odoo.insurance.covered.base.mode:full}")
    private String coveredBaseMode;

    @Value("${odoo.insurance.coverage.model:insurance.product.coverage}")
    private String coverageModel;

    @Autowired
    private OdooClient odooClient;

    public boolean isEnabled() {
        return insuranceEnabled;
    }

    public boolean isAddonEnabled() {
        return insuranceEnabled && addonEnabled;
    }

    /**
     * Fail-closed validation of the insurance coverage tier (issue #189): a patient with a value
     * outside the configured tier set is rejected. A patient WITHOUT the attribute is rejected by
     * default (policy {@code reject}); with {@code odoo.insurance.missing.tier.policy=skip} the
     * patient is instead treated as uninsured and {@code -1} is returned, so the sync proceeds
     * without insurance processing (legacy patients predating the attribute keep working).
     * Callers must gate on {@link #isEnabled()} — when the integration is disabled this method
     * throws, it never silently accepts.
     */
    public int validateCoverageTier(Patient patient) {
        if (!insuranceEnabled) {
            throw new EIPException("Insurance coverage integration is disabled");
        }
        String tier = extractCoverageTier(patient);
        if (tier == null || tier.isBlank()) {
            if (MISSING_TIER_POLICY_SKIP.equalsIgnoreCase(missingTierPolicy)) {
                log.warn(
                        "Patient {} is missing required {} person attribute; treating as uninsured "
                                + "(missing-tier policy=skip)",
                        patient.getIdPart(),
                        insuranceAttributeName);
                return NO_TIER;
            }
            throw new EIPException(String.format(
                    "Patient %s is missing required %s person attribute", patient.getIdPart(), insuranceAttributeName));
        }
        for (int percent : getTierSet()) {
            if (matchesTier(tier.trim(), percent)) {
                log.info("Patient {} has insurance coverage tier {}%", patient.getIdPart(), percent);
                return percent;
            }
        }
        throw new EIPException(String.format(
                "Unsupported %s value %s for patient %s", insuranceAttributeName, tier, patient.getIdPart()));
    }

    private boolean matchesTier(String tier, int percent) {
        return tier.equals(String.valueOf(percent))
                || tier.equals(percent + "%")
                || tier.equals(String.format(planNameTemplate, percent));
    }

    private Set<Integer> getTierSet() {
        Set<Integer> tiers = new LinkedHashSet<>();
        for (String raw : configuredTiers.split(",")) {
            tiers.add(Integer.parseInt(raw.trim()));
        }
        return tiers;
    }

    /**
     * Mirrors the insurance tier into the insurance_coverage addon model (issue #184): find-or-create
     * the base plan partner for the tier, enrol the patient in it and make sure every saleable product
     * has a coverage row for the plan (coverage_percentage = tier, covered_base_mode = configured).
     * The addon splits FULL-priced invoices between the payers, so this deliberately does not assign a
     * discount pricelist to the partner. Idempotent by plan ref and the (insurance_id, product_id)
     * unique constraint, so calling it on BOTH the plain patient sync and the order-driven
     * (createOrUpdatePartner) path is safe — the second call re-ensures the same plan/coverage rows.
     * No-op unless both the integration and the addon mirror are enabled.
     */
    public void applyAddonModelCoverage(Patient patient, Partner partner) {
        if (!isAddonEnabled()) {
            return;
        }
        int percent = validateCoverageTier(patient);
        if (percent <= 0) {
            // Missing tier with the skip policy: no plan, no enrolment, no coverage rows.
            return;
        }
        String planName = String.format(planNameTemplate, percent);
        String planRef = planRefPrefix + percent;
        int planId = findOrCreateInsurancePlan(planName, planRef);
        partner.setPartnerBaseInsuranceId(planId);
        ensureCoverageRows(planId, percent);
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
                coverageModel, List.of(asList("insurance_id", "=", planId)), List.of("product_id"));
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
                    coveredBaseMode));
        }
        for (Map<String, Object> row : coverageRows) {
            Integer created = odooClient.create(coverageModel, List.of(row));
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
            if (insuranceAttributeName.equals(attributeName)) {
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
}
