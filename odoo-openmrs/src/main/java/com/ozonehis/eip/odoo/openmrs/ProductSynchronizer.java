/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.odoo.openmrs.client.OdooFhirClient;
import com.ozonehis.eip.odoo.openmrs.client.OpenmrsRestClient;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Medication.MedicationStatus;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class ProductSynchronizer {

    private OdooFhirClient odooFhirClient;

    private IGenericClient openmrsFhirClient;

    private OpenmrsRestClient openmrsRestClient;

    private DataSource openmrsDataSource;

    public ProductSynchronizer(
            OdooFhirClient odooFhirClient,
            IGenericClient openmrsFhirClient,
            OpenmrsRestClient openmrsRestClient,
            DataSource openmrsDataSource) {
        this.odooFhirClient = odooFhirClient;
        this.openmrsFhirClient = openmrsFhirClient;
        this.openmrsRestClient = openmrsRestClient;
        this.openmrsDataSource = openmrsDataSource;
    }

    @Scheduled(initialDelayString = "${eip.product.sync.initial.delay}", fixedDelayString = "${eip.product.sync.delay}")
    public void syncProducts() throws Exception {
        Bundle bundle = odooFhirClient.getAll(Medication.class);
        log.info("Read {} drugs from Odoo to sync", bundle.getEntry().size());

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Medication medication = (Medication) entry.getResource();
            final String uuid = medication.getIdElement().getIdPart();
            byte[] resp = openmrsRestClient.get("drug", uuid);
            Extension e = medication
                    .getExtensionByUrl(Constants.FHIR_OPENMRS_FHIR_EXT_MEDICINE)
                    .getExtensionByUrl(Constants.FHIR_OPENMRS_EXT_DRUG_NAME);
            Coding coding = medication.getCode().getCoding().get(0);
            String sourceName = ProductSyncUtils.getConceptSourceName(coding.getSystem(), openmrsDataSource);
            Map<String, Object> map = new HashMap<>();
            map.put("uuid", uuid);
            map.put("name", e.getValue().toString());
            map.put("concept", sourceName + ":" + coding.getCode());
            map.put("combination", false);
            String body = new ObjectMapper().writeValueAsString(map);
            String resourceUuid = null;
            if (resp == null) {
                log.info("Creating new drug in OpenMRS");
            } else {
                log.info("Updating existing drug in OpenMRS");
                resourceUuid = uuid;
                if (medication.getStatus() == MedicationStatus.ACTIVE) {
                    // TODO If inactive, delete(retire)
                }
            }

            openmrsRestClient.createOrUpdate("drug", resourceUuid, body);
        }
    }
}
