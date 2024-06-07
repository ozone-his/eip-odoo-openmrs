/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper.fhir;

import com.ozonehis.eip.odooopenmrs.mapper.ToFhirMapping;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper implements ToFhirMapping<Patient, Partner> {

    @Override
    public Patient toFhir(Partner odooDocument) {
        Patient patient = new Patient();
        patient.setId(odooDocument.getPartnerRef()); // This is the patient's ID in the FHIR server
        return patient;
    }
}