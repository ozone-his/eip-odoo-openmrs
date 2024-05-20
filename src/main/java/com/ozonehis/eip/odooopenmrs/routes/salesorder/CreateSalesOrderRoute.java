/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes.salesorder;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CreateSalesOrderRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:odoo-update-sales-order-route")
                .routeId("odoo-update-sales-order-route")
                .to("odoo://write/sale.order")
                .log(LoggingLevel.INFO, "Sales order updated.")
                .end();
        // spotless:on
    }
}
