package com.ozonehis.eip.odooopenmrs.route;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({
    "org.openmrs.eip", "com.ozonehis.eip"
})
public class TestConfig {}
