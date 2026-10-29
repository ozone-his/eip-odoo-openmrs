/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import static com.ozonehis.eip.odoo.openmrs.VisitTestUtils.visit;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.processors.VisitProcessor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

class EncounterRoutingTest {
    @Test
    void forwardsFhirEncountersToVisitProcessor() throws Exception {
        VisitProcessor visits = mock(VisitProcessor.class);
        EncounterRouting route = new EncounterRouting();
        route.setVisitProcessor(visits);
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.addRoutes(route);
            context.start();
            try (ProducerTemplate producer = context.createProducerTemplate()) {
                producer.sendBodyAndHeader(
                        "direct:fhir-encounter", visit("2026-10-22T00:00:00Z", null), HEADER_FHIR_EVENT_TYPE, "c");
                verify(visits).process(any());
                producer.sendBody("direct:fhir-encounter", null);
                verifyNoMoreInteractions(visits);
            }
        }
    }
}
