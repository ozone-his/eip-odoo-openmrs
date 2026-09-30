/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.InsuranceCoverageHandler;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class InsuranceCoverageHandlerTest {

    private static final String PATIENT_ID = "5td050e1-c1be-4b4c-b407-c48d2db49b65";

    @InjectMocks
    private InsuranceCoverageHandler insuranceCoverageHandler;

    @Mock
    private OdooClient odooClient;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
        insuranceCoverageHandler.setInsuranceAttributeName("Insurance Coverage Tier");
        insuranceCoverageHandler.setPlanNameTemplate("Insurance %d%%");
        insuranceCoverageHandler.setConfiguredTiers("50,60,70,80,90,100");
    }

    @Test
    public void shouldBeDisabledByDefault() {
        assertFalse(insuranceCoverageHandler.isEnabled());
        assertThrows(EIPException.class, () -> insuranceCoverageHandler.validateCoverageTier(patientWithTier("50%")));
    }

    @Test
    public void shouldAcceptEveryConfiguredTier() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        for (String tier : List.of("50", "50%", "Insurance 50%")) {
            assertEquals(50, insuranceCoverageHandler.validateCoverageTier(patientWithTier(tier)));
        }
        for (String tier : List.of("60%", "70%", "80%", "90%", "100%")) {
            int expected = Integer.parseInt(tier.replace("%", ""));
            assertEquals(expected, insuranceCoverageHandler.validateCoverageTier(patientWithTier(tier)));
        }
    }

    @Test
    public void shouldFailClosedForMissingBlankAndUnsupportedTier() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        for (String tier : asList(null, "", "   ", "95%")) {
            Patient patient = patientWithTier(tier);
            EIPException error =
                    assertThrows(EIPException.class, () -> insuranceCoverageHandler.validateCoverageTier(patient));
            assertNotNull(error.getMessage());
        }
    }

    @Test
    public void shouldSkipValidationWhenTierMissingAndPolicyIsSkip() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        insuranceCoverageHandler.setMissingTierPolicy("skip");

        assertEquals(-1, insuranceCoverageHandler.validateCoverageTier(patientWithTier(null)));
        assertEquals(-1, insuranceCoverageHandler.validateCoverageTier(patientWithTier("   ")));
    }

    @Test
    public void shouldHonorCustomTierSet() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        insuranceCoverageHandler.setConfiguredTiers("25,50,75,100");
        assertEquals(75, insuranceCoverageHandler.validateCoverageTier(patientWithTier("75%")));
        assertThrows(EIPException.class, () -> insuranceCoverageHandler.validateCoverageTier(patientWithTier("60%")));
    }

    @Test
    public void shouldMatchPlanNameAliasFromCustomTemplate() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        insuranceCoverageHandler.setPlanNameTemplate("Mutuelles %d%%");
        assertEquals(60, insuranceCoverageHandler.validateCoverageTier(patientWithTier("Mutuelles 60%")));
    }

    @Test
    public void shouldCreatePlanAndCoverageForTierWhenAddonMirrorEnabled() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        insuranceCoverageHandler.setAddonEnabled(true);
        insuranceCoverageHandler.setPlanRefPrefix("INS-");
        insuranceCoverageHandler.setCoveredBaseMode("full");
        insuranceCoverageHandler.setCoverageModel("insurance.product.coverage");
        Patient patient = patientWithTier("50%");
        Partner partner = new Partner();
        when(odooClient.searchAndRead(eq(Constants.PARTNER_MODEL), eq(List.of(asList("ref", "=", "INS-50"))), any()))
                .thenReturn(new Object[] {});
        when(odooClient.searchAndRead(eq("product.product"), any(), any()))
                .thenReturn(new Object[] {Map.of("id", 1), Map.of("id", 2)});
        when(odooClient.searchAndRead(eq("insurance.product.coverage"), any(), any()))
                .thenReturn(new Object[] {});
        when(odooClient.create(eq(Constants.PARTNER_MODEL), any())).thenReturn(42);
        when(odooClient.create(eq("insurance.product.coverage"), any())).thenReturn(1, 2);

        insuranceCoverageHandler.applyAddonModelCoverage(patient, partner);

        assertEquals(42, partner.getPartnerBaseInsuranceId());
        verify(odooClient)
                .create(
                        eq(Constants.PARTNER_MODEL),
                        eq(List.of(Map.of(
                                "name", "Insurance 50%",
                                "ref", "INS-50",
                                "is_insurance", true,
                                "insurance_type", "base"))));
        verify(odooClient)
                .create(
                        eq("insurance.product.coverage"),
                        eq(List.of(Map.of(
                                "insurance_id",
                                42,
                                "product_id",
                                1,
                                "coverage_percentage",
                                50.0,
                                "covered_base_mode",
                                "full"))));
        verify(odooClient)
                .create(
                        eq("insurance.product.coverage"),
                        eq(List.of(Map.of(
                                "insurance_id",
                                42,
                                "product_id",
                                2,
                                "coverage_percentage",
                                50.0,
                                "covered_base_mode",
                                "full"))));
    }

    @Test
    public void shouldNotDuplicatePlanOrCoverageWhenAddonMirrorEnabled() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        insuranceCoverageHandler.setAddonEnabled(true);
        insuranceCoverageHandler.setPlanRefPrefix("INS-");
        insuranceCoverageHandler.setCoveredBaseMode("full");
        insuranceCoverageHandler.setCoverageModel("insurance.product.coverage");
        Patient patient = patientWithTier("100%");
        Partner partner = new Partner();
        when(odooClient.searchAndRead(eq(Constants.PARTNER_MODEL), eq(List.of(asList("ref", "=", "INS-100"))), any()))
                .thenReturn(new Object[] {Map.of("id", 7, "name", "Insurance 100%")});
        when(odooClient.searchAndRead(eq("product.product"), any(), any()))
                .thenReturn(new Object[] {Map.of("id", 1), Map.of("id", 2)});
        when(odooClient.searchAndRead(eq("insurance.product.coverage"), any(), any()))
                .thenReturn(new Object[] {Map.of("product_id", new Object[] {1, "P1"})});
        when(odooClient.create(eq("insurance.product.coverage"), any())).thenReturn(3);

        insuranceCoverageHandler.applyAddonModelCoverage(patient, partner);

        assertEquals(7, partner.getPartnerBaseInsuranceId());
        verify(odooClient, never()).create(eq(Constants.PARTNER_MODEL), any());
        verify(odooClient, times(1)).create(eq("insurance.product.coverage"), any());
        verify(odooClient)
                .create(
                        eq("insurance.product.coverage"),
                        eq(List.of(Map.of(
                                "insurance_id",
                                7,
                                "product_id",
                                2,
                                "coverage_percentage",
                                100.0,
                                "covered_base_mode",
                                "full"))));
    }

    @Test
    public void shouldDoNothingWhenAddonMirrorDisabled() {
        Patient patient = patientWithTier("50%");
        Partner partner = new Partner();

        insuranceCoverageHandler.applyAddonModelCoverage(patient, partner);

        assertNull(partner.getPartnerBaseInsuranceId());
        verify(odooClient, never()).searchAndRead(any(), any(), any());
        verify(odooClient, never()).create(any(), any());
    }

    @Test
    public void shouldNotMirrorWhenTierMissingAndPolicyIsSkip() {
        insuranceCoverageHandler.setInsuranceEnabled(true);
        insuranceCoverageHandler.setAddonEnabled(true);
        insuranceCoverageHandler.setMissingTierPolicy("skip");
        insuranceCoverageHandler.setPlanRefPrefix("INS-");
        insuranceCoverageHandler.setCoveredBaseMode("full");
        insuranceCoverageHandler.setCoverageModel("insurance.product.coverage");
        Partner partner = new Partner();

        insuranceCoverageHandler.applyAddonModelCoverage(patientWithTier(null), partner);

        assertNull(partner.getPartnerBaseInsuranceId());
        verify(odooClient, never()).searchAndRead(any(), any(), any());
        verify(odooClient, never()).create(any(), any());
    }

    private Patient patientWithTier(String tier) {
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        if (tier != null) {
            addInsuranceTier(patient, tier);
        }
        return patient;
    }

    private void addInsuranceTier(Patient patient, String tier) {
        Extension attribute = new Extension("http://fhir.openmrs.org/ext/person-attribute");
        patient.addExtension(attribute);
        attribute.addExtension(new Extension(
                "http://fhir.openmrs.org/ext/person-attribute-type", new StringType("Insurance Coverage Tier")));
        attribute.addExtension(new Extension(
                "http://fhir.openmrs.org/ext/person-attribute-value", new CodeableConcept().setText(tier)));
    }
}
