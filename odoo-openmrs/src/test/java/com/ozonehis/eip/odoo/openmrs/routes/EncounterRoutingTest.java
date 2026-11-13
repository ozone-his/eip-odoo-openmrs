/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.processors.EncounterProcessor;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.Test;

class EncounterRoutingTest {
    @Test
    void forwardsUuidOnlyDeleteEventToProcessor() throws Exception {
        EncounterProcessor processor = mock(EncounterProcessor.class);
        EncounterRouting route = new EncounterRouting();
        route.setEncounterProcessor(processor);
        try (DefaultCamelContext context = new DefaultCamelContext()) {
            context.addRoutes(route);
            context.start();
            try (var producer = context.createProducerTemplate()) {
                producer.requestBodyAndHeader("direct:fhir-encounter", "visit-123", HEADER_FHIR_EVENT_TYPE, "d");
            }
            verify(processor).process(any());
        }
    }
}
