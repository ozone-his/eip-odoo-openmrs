/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import java.time.Instant;
import java.util.Date;
import org.hl7.fhir.r4.model.Encounter;

public final class VisitTestUtils {

    private VisitTestUtils() {}

    public static Encounter visit(String start, String end) {
        Encounter visit = new Encounter();
        visit.setId("visit-123");
        visit.getMeta().addTag(VisitUtils.ENCOUNTER_TAG_SYSTEM, "visit", "Visit");
        visit.addType()
                .addCoding()
                .setSystem(VisitUtils.VISIT_TYPE_SYSTEM)
                .setCode("type-123")
                .setDisplay("Inpatient");
        visit.getPeriod().setStart(Date.from(Instant.parse(start)));
        if (end != null) {
            visit.getPeriod().setEnd(Date.from(Instant.parse(end)));
        }
        return visit;
    }
}
