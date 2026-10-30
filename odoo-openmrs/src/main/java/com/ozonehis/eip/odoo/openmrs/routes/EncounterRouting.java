/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import com.ozonehis.eip.odoo.openmrs.processors.EncounterProcessor;
import lombok.Setter;
import org.apache.camel.builder.RouteBuilder;
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
                .process(encounterProcessor)
                .end();
    }
}
