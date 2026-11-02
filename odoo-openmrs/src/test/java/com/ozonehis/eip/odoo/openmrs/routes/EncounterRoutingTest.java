/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.processors.EncounterProcessor;
import java.time.Instant;
import java.util.Date;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.hl7.fhir.r4.model.Encounter;
import org.junit.jupiter.api.Test;

class EncounterRoutingTest {
    @Test
    void forwardsFhirEncountersToEncounterProcessor() throws Exception {
        EncounterProcessor encounters = mock(EncounterProcessor.class);
        EncounterRouting route = new EncounterRouting();
        route.setEncounterProcessor(encounters);
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.addRoutes(route);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                producer.sendBodyAndHeader(
                        "direct:fhir-encounter", visit("2026-10-22T00:00:00Z", null), HEADER_FHIR_EVENT_TYPE, "c");
                verify(encounters).process(any());
                producer.sendBody("direct:fhir-encounter", null);
                verifyNoMoreInteractions(encounters);
            }
        }
    }

    public Encounter visit(String start, String end) {
        Encounter visit = new Encounter();
        visit.setId("visit-123");
        visit.addType()
                .addCoding()
                .setSystem(Constants.VISIT_TYPE_SYSTEM)
                .setCode("type-123")
                .setDisplay("Inpatient");
        visit.getPeriod().setStart(Date.from(Instant.parse(start)));
        if (end != null) {
            visit.getPeriod().setEnd(Date.from(Instant.parse(end)));
        }
        return visit;
    }
}
