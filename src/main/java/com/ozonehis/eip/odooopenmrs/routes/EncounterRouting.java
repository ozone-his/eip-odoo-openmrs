/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.processors.EncounterProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncounterRouting extends RouteBuilder {

    @Autowired
    private EncounterProcessor encounterProcessor;

    @Override
    public void configure() throws Exception {
        // spotless:off
        from("direct:encounter-to-sale-order-router")
                .routeId("encounter-to-sale-order-router")
                .process(encounterProcessor)
                .choice()
                .when(simple("${exchangeProperty." + Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER + "} == true"))
                .log(LoggingLevel.INFO, "Skipping encounter processing")
                .otherwise()
                .log(LoggingLevel.INFO, "Processing encounter")
                .end()
                .end();

        from("direct:fhir-encounter")
                .routeId("fhir-encounter-to-sale-order-router")
                .filter(body().isNotNull())
                .filter(exchange -> exchange.getMessage().getBody() instanceof Encounter)
                .to("direct:encounter-to-sale-order-router")
                .end();
        // spotless:on
    }
}
