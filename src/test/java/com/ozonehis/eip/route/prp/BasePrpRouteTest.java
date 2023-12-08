package com.ozonehis.eip.route.prp;

import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC;
import static com.ozonehis.eip.route.OdooTestConstants.ROUTE_ID_GET_HSU_ID;

import com.ozonehis.eip.route.BaseOdooRouteTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
        properties = "camel.springboot.xml-routes=classpath*:camel/prp/*.xml,classpath*:camel/"
                + ROUTE_ID_GET_CONCEPT_BY_UUID_FROM_ENC + ".xml,classpath*:camel/" + ROUTE_ID_GET_HSU_ID + ".xml")
@TestPropertySource(properties = "eip.watchedTables=person,obs")
@TestPropertySource(properties = "odoo.handler.route=odoo-prp-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=person:person,obs:obs")
public abstract class BasePrpRouteTest extends BaseOdooRouteTest {}
