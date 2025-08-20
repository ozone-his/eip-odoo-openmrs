/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooFhirClient;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Medication;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class ProductSynchronizer {

    private OdooFhirClient odooFhirClient;

    private IGenericClient openmrsFhirClient;

    public ProductSynchronizer(OdooFhirClient odooFhirClient, IGenericClient openmrsFhirClient) {
        this.odooFhirClient = odooFhirClient;
        this.openmrsFhirClient = openmrsFhirClient;
    }

    @Scheduled(initialDelayString = "${eip.product.sync.initial.delay}", fixedDelayString = "${eip.product.sync.delay}")
    public void syncProducts() {
        Bundle bundle = odooFhirClient.getAll(Medication.class);
        log.info("Read {} drugs from Odoo to sync: {}", bundle.getEntry().size());
    }
}
