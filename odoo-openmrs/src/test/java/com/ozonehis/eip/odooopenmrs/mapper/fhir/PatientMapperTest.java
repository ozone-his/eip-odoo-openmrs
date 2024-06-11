/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper.fhir;

import static org.junit.jupiter.api.Assertions.*;

import com.ozonehis.eip.odooopenmrs.model.Partner;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PatientMapperTest {

    private PatientMapper patientMapper;

    @BeforeEach
    public void setup() {
        patientMapper = new PatientMapper();
    }

    @Test
    public void shouldMapOdooPartnerToFhirPatient() {
        // Setup
        Partner partner = new Partner();
        partner.setPartnerRef("123");

        // Act
        Patient patient = patientMapper.toFhir(partner);

        // Assert
        assertEquals("123", patient.getId());
    }
}
