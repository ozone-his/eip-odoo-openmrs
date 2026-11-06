/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.ozonehis.eip.odoo.openmrs.Constants;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class EncounterHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public boolean isVisit(Encounter encounter) {
        if (encounter == null) {
            return false;
        }
        for (CodeableConcept type : encounter.getType()) {
            for (Coding coding : type.getCoding()) {
                if (Constants.VISIT_TYPE_SYSTEM.equals(coding.getSystem())) {
                    return true;
                }
            }
        }
        return false;
    }

    public Coding getVisitType(Encounter encounter) {
        for (CodeableConcept type : encounter.getType()) {
            for (Coding coding : type.getCoding()) {
                if (Constants.VISIT_TYPE_SYSTEM.equals(coding.getSystem()) && coding.hasCode()) {
                    return coding;
                }
            }
        }
        throw new IllegalArgumentException("Visit " + encounter.getIdPart() + " has no visit type");
    }

    public Encounter getEncounterByEncounterID(String encounterID) {
        Encounter encounter = openmrsFhirClient
                .read()
                .resource(Encounter.class)
                .withId(encounterID)
                .execute();

        log.info("EncounterHandler: Encounter getEncounterByEncounterID {}", encounter.getId());
        return encounter;
    }
}
