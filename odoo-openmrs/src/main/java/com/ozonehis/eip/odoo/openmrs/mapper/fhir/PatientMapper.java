/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper.fhir;

import com.ozonehis.eip.odoo.openmrs.mapper.ToFhirMapping;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper implements ToFhirMapping<Patient, Partner> {

    @Override
    public Patient toFhir(Partner odooResource) {
        Patient patient = new Patient();
        patient.setId(odooResource.getPartnerRef()); // This is the patient's ID in the FHIR server
        return patient;
    }
}
