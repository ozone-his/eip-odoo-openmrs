/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;

public final class VisitUtils {

    public static final String ENCOUNTER_TAG_SYSTEM = "http://fhir.openmrs.org/ext/encounter-tag";

    public static final String VISIT_TYPE_SYSTEM = "http://fhir.openmrs.org/code-system/visit-type";

    private VisitUtils() {}

    public static boolean isVisit(Encounter encounter) {
        return encounter != null
                && encounter.getMeta().getTag().stream()
                        .anyMatch(tag -> ENCOUNTER_TAG_SYSTEM.equals(tag.getSystem()) && "visit".equals(tag.getCode()));
    }

    public static Coding getVisitType(Encounter visit) {
        return visit.getType().stream()
                .flatMap(type -> type.getCoding().stream())
                .filter(coding -> VISIT_TYPE_SYSTEM.equals(coding.getSystem()) && coding.hasCode())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Visit " + visit.getIdPart() + " has no visit type"));
    }
}
