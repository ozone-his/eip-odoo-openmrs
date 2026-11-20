/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.EncounterHandler;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.Test;

class EncounterHandlerTest {

    private final EncounterHandler handler = new EncounterHandler();

    @Test
    void recognizesVisitTypeWithoutMetadata() {
        Encounter encounter = new Encounter();
        encounter
                .addType()
                .addCoding()
                .setSystem(Constants.VISIT_TYPE_SYSTEM)
                .setCode("7b0f5697-27e3-40c4-8bae-f4049abfb4ed")
                .setDisplay("Facility Visit");

        assertTrue(handler.isVisit(encounter));
    }

    @Test
    void recognizesVisitTypeAmongMultipleTypesAndCodings() {
        Encounter encounter = new Encounter();
        encounter.addType().setText("Other type");
        CodeableConcept type = encounter.addType();
        type.addCoding().setSystem("http://example.org/types").setCode("other");
        type.addCoding().setSystem(Constants.VISIT_TYPE_SYSTEM).setCode("another-visit-type");

        assertTrue(handler.isVisit(encounter));
    }

    @Test
    void rejectsVisitCodingWithoutSystem() {
        Encounter encounter = new Encounter();
        encounter
                .addType()
                .addCoding()
                .setCode("7b0f5697-27e3-40c4-8bae-f4049abfb4ed")
                .setDisplay("Facility Visit");

        assertFalse(handler.isVisit(encounter));
    }

    @Test
    void rejectsOtherCodingSystems() {
        Encounter encounter = new Encounter();
        encounter
                .addType()
                .addCoding()
                .setSystem("http://fhir.openmrs.org/code-system/encounter-type")
                .setCode("7b0f5697-27e3-40c4-8bae-f4049abfb4ed")
                .setDisplay("Facility Visit");

        assertFalse(handler.isVisit(encounter));
    }

    @Test
    void rejectsNullOrMissingTypeCoding() {
        assertFalse(handler.isVisit(null));
        Encounter encounter = new Encounter();
        assertFalse(handler.isVisit(encounter));
        encounter.addType().setText("Facility Visit");
        assertFalse(handler.isVisit(encounter));
        encounter.getTypeFirstRep().addCoding().setCode("visit");
        assertFalse(handler.isVisit(encounter));
    }
}
