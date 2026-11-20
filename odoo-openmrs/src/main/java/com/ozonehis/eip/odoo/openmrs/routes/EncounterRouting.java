/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.processors.EncounterProcessor;
import lombok.Setter;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter
public class EncounterRouting extends RouteBuilder {

    @Autowired
    private EncounterProcessor encounterProcessor;

    @Override
    public void configure() throws Exception {
        from("direct:fhir-encounter")
                .routeId("fhir-encounter-to-sale-order-router")
                .filter(body().isNotNull())
                .filter(exchange -> "d".equals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE, String.class))
                        || exchange.getMessage().getBody() instanceof Encounter)
                .process(encounterProcessor)
                .end();
    }
}
