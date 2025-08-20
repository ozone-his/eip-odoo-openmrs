/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.config;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.ozonehis.eip.odoo.openmrs.ProductSynchronizer;
import com.ozonehis.eip.odoo.openmrs.client.OdooFhirClient;
import org.openmrs.eip.app.config.AppConfig;
import org.openmrs.eip.fhir.spring.OpenmrsFhirAppConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Import the {@link AppConfig} class to ensure that the required beans are created.
 */
@Configuration
@Import({AppConfig.class, OpenmrsFhirAppConfig.class})
@EnableScheduling
public class EIPAppConfig {

    @Bean
    public ProductSynchronizer productCatalogSynchronizer(
            OdooFhirClient odooFhirClient, IGenericClient openmrsFhirClient) {
        return new ProductSynchronizer(odooFhirClient, openmrsFhirClient);
    }
}
