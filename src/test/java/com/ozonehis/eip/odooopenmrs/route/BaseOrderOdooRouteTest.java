package com.ozonehis.eip.odooopenmrs.route;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties =
                "eip.watchedTables=patient,person_name,person_address,patient_identifier,orders,test_order,drug_order")
@TestPropertySource(properties = "odoo.handler.route=odoo-order-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=orders:order,test_order:order,drug_order:order")
public abstract class BaseOrderOdooRouteTest extends BaseOdooRouteTest {}
