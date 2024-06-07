/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs;

import static java.util.Arrays.asList;

import java.util.List;

public class Constants {
    public static final String PARTNER_MODEL = "res.partner";

    public static final String SALE_ORDER_MODEL = "sale.order";

    public static final String SALE_ORDER_LINE_MODEL = "sale.order.line";

    public static final String COUNTRY_STATE_MODEL = "res.country.state";

    public static final String COUNTRY_MODEL = "res.country";

    public static final String UOM_MODEL = "uom.uom";

    public static final String PRODUCT_MODEL = "product.product";

    public static final String IR_MODEL = "ir.model.data";

    public static final String CREATE_METHOD = "create";

    public static final String WRITE_METHOD = "write";

    public static final String UNLINK_METHOD = "unlink";

    public static final String HEADER_ODOO_ID_ATTRIBUTE_VALUE = "odoo.attribute.value";

    public static final String EXCHANGE_PROPERTY_SKIP_ENCOUNTER = "skip.encounter";

    public static final String FHIR_RESOURCE_TYPE = "fhir.resource.type";

    public static final String HEADER_ENABLE_PATIENT_SYNC = "enable.patient.sync";

    public static final List<String> orderDefaultAttributes =
            asList("id", "client_order_ref", "partner_id", "state", "order_line");

    public static final List<String> partnerDefaultAttributes =
            asList("id", "name", "ref", "street", "street2", "city", "zip", "active", "comment");
}
