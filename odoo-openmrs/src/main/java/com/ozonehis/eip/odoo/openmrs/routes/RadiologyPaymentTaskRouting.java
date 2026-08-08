/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import com.ozonehis.eip.odoo.openmrs.processors.RadiologyPaymentTaskProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class RadiologyPaymentTaskRouting extends RouteBuilder {

    private static final String SCHEDULE = "scheduler:radiology-payment-task-poll?initialDelay=60000&delay=30000";

    @Autowired
    private RadiologyPaymentTaskProcessor radiologyPaymentTaskProcessor;

    @Override
    public void configure() {
        from(SCHEDULE)
                .routeId("radiology-payment-task-poll")
                .log(LoggingLevel.INFO, "Polling for active radiology ServiceRequests to check Odoo payment status...")
                .process(radiologyPaymentTaskProcessor)
                .log(LoggingLevel.INFO, "Radiology payment Task check complete.")
                .end();
    }
}
