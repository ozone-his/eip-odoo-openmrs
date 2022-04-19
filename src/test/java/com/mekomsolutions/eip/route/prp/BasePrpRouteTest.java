package com.mekomsolutions.eip.route.prp;

import static com.mekomsolutions.eip.route.OdooTestConstants.ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC;

import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.BaseOdooRouteTest;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/prp/*.xml,classpath*:camel/"
        + ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC + ".xml")
@TestPropertySource(properties = "eip.watchedTables=person,obs")
@TestPropertySource(properties = "odoo.handler.route=odoo-prp-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=person:person,obs:obs")
public abstract class BasePrpRouteTest extends BaseOdooRouteTest {}
