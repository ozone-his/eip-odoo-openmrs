package com.mekomsolutions.eip.route.prp;

import org.springframework.test.context.TestPropertySource;

import com.mekomsolutions.eip.route.BaseOdooRouteTest;

@TestPropertySource(properties = "camel.springboot.xml-routes=classpath*:camel/*.xml,classpath*:camel/prp/*.xml")
@TestPropertySource(properties = "eip.watchedTables=person,obs")
@TestPropertySource(properties = "odoo.handler.route=odoo-prp-handler")
@TestPropertySource(properties = "odoo.custom.table.resource.mappings=person:person,obs:obs")
public abstract class BasePrpRouteTest extends BaseOdooRouteTest {}
