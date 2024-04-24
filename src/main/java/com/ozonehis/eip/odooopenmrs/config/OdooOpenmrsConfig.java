package com.ozonehis.eip.odooopenmrs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(
        value = {"eip-odoo-openmrs.properties", "classpath:eip-odoo-openmrs.properties"},
        ignoreResourceNotFound = true)
public class OdooOpenmrsConfig {}
