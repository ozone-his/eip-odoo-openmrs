/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes.partner;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class UpdatePartnerRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
		from("direct:odoo-update-partner-route")
				.log(LoggingLevel.INFO, "Updating Partner in Odoo...")
			.routeId("odoo-update-partner-route")
			.to("odoo://write/res.partner")
			.end();
		// spotless:on
    }
}
