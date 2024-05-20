package com.ozonehis.eip.odooopenmrs;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;

public class Constants {
    public static final String PARTNER_MODEL = "res.partner";

    public static final String SALE_ORDER_MODEL = "sale.order";

    public static final String SALE_ORDER_LINE_MODEL = "sale.order.line";

    public static final String COUNTRY_STATE_MODEL = "res.country.state";

    public static final String COUNTRY_MODEL = "res.country";

    public static final String SEARCH_METHOD = "search";

    public static final String SEARCH_READ_METHOD = "search";

    public static final String HEADER_ODOO_RESOURCE = "Odoo.resource";

    public static final String HEADER_ODOO_DOCTYPE = "Odoo.doctype";

    public static final String HEADER_ODOO_ID = "Odoo.id";

    public static final String EXCHANGE_PROPERTY_SKIP_ENCOUNTER = "skip.encounter";

    public static final String FHIR_RESOURCE_TYPE = "fhir.resource.type";

    public static final String HEADER_EVENT_PROCESSED = "odoo.openmrs.event.processed";


    public List<String> orderDefaultAttributes = asList("name", "amount_total", "state", "pricelist_id", "payment_term_id",
            "invoice_status", "origin", "create_date", "currency_id", "order_line", "invoice_count", "invoice_ids",
            "product_id");

    public List<String> partnerDefaultAttributes = Arrays.asList("id", "name", "ref", "create_date");

}