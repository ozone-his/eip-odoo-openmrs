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
        return encounter != null
                && encounter.getMeta().getTag().stream()
                        .anyMatch(tag -> Constants.ENCOUNTER_TAG_SYSTEM.equals(tag.getSystem())
                                && "visit".equals(tag.getCode()));
    }

    public Coding getVisitType(Encounter encounter) {
        return encounter.getType().stream()
                .flatMap(type -> type.getCoding().stream())
                .filter(coding -> Constants.VISIT_TYPE_SYSTEM.equals(coding.getSystem()) && coding.hasCode())
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Visit " + encounter.getIdPart() + " has no visit type"));
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
