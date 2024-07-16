/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes.partner;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CreatePartnerRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:odoo-create-partner-route")
                .log(LoggingLevel.INFO, "Creating Partner in Odoo...")
                .routeId("odoo-create-partner-route")
                .to("odoo://create/res.partner")
                .end();
        // spotless:on
    }
}
