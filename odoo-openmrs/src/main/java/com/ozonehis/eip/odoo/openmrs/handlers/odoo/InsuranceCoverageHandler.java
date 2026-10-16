/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.odoo;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.eip.EIPException;
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

    public boolean isEnabled() {
        return insuranceEnabled;
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
