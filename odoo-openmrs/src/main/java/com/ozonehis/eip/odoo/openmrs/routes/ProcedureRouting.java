/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import com.ozonehis.eip.odoo.openmrs.processors.ServiceRequestProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ProcedureRouting extends RouteBuilder {

    @Autowired
    private ServiceRequestProcessor serviceRequestProcessor;

    @Override
    public void configure() {
        // spotless:off
        from("direct:fhir-procedure")
                .routeId("procedure-to-sale-order-router")
                .filter(body().isNotNull())
                .log(LoggingLevel.INFO, "Processing Procedure router")
                .process(exchange -> {
                    log.info("Procedure body {}", exchange.getMessage().getBody(String.class));
                })                .to("direct:procedure-to-sale-order-processor")
                .end();

        from("direct:procedure-to-sale-order-processor")
                .routeId("procedure-to-sale-order-processor")
                .process(serviceRequestProcessor)
                .log(LoggingLevel.INFO, "Processing Procedure")
                .end();
        // spotless:on
    }
}
