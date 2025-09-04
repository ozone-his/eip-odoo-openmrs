/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

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

    protected static final String RESOURCE = "drug";

    private static ObjectMapper MAPPER = new ObjectMapper();

    private OdooFhirClient odooFhirClient;

    private OpenmrsRestClient openmrsRestClient;

    private DataSource openmrsDataSource;

    public ProductSynchronizer(
            OdooFhirClient odooFhirClient, OpenmrsRestClient openmrsRestClient, DataSource openmrsDataSource) {
        this.odooFhirClient = odooFhirClient;
        this.openmrsRestClient = openmrsRestClient;
        this.openmrsDataSource = openmrsDataSource;
    }

    @Scheduled(initialDelayString = "${eip.product.sync.initial.delay}", fixedDelayString = "${eip.product.sync.delay}")
    public void syncProducts() throws Exception {
        Bundle bundle = odooFhirClient.getAll(Medication.class);
        log.info("Read {} drugs from Odoo to sync", bundle.getEntry().size());

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Medication medication = (Medication) entry.getResource();
            final String id = medication.getIdElement().getIdPart();
            Extension medExt = medication.getExtensionByUrl(Constants.FHIR_OPENMRS_FHIR_EXT_MEDICINE);
            Extension nameExt = medExt.getExtensionByUrl(Constants.FHIR_OPENMRS_EXT_DRUG_NAME);
            Map<String, Object> drugData = new HashMap<>();
            drugData.put("name", nameExt.getValue().toString());
            if (medication.getCode().getCoding().size() == 1) {
                Coding coding = medication.getCode().getCoding().get(0);
                String sourceName = ProductSyncUtils.getConceptSourceName(coding.getSystem(), openmrsDataSource);
                drugData.put("concept", sourceName + ":" + coding.getCode());
            }

            Extension strengthExt = medExt.getExtensionByUrl(Constants.FHIR_OPENMRS_EXT_DRUG_STRENGTH);
            if (strengthExt != null) {
                drugData.put("strength", strengthExt.getValue().toString());
            }

            String uuid = null;
            byte[] drug = openmrsRestClient.get(RESOURCE, id);
            if (drug == null && medication.getStatus() == MedicationStatus.INACTIVE) {
                log.info("Skipping new inactive drug");
                continue;
            }

            boolean isRetired = false;
            if (drug == null) {
                if (medication.getCode().getCoding().isEmpty()) {
                    log.warn("Skipping new drug with external id {} because of missing concept mapping", id);
                    continue;
                }

                log.info("Creating new drug in OpenMRS with uuid {}", id);
                drugData.put("uuid", id);
                drugData.put("combination", false);
            } else {
                uuid = id;
                log.info("Updating existing drug in OpenMRS with uuid {}", uuid);
                Map<String, Object> drugMap = MAPPER.readValue(drug, Map.class);
                isRetired = Boolean.valueOf(drugMap.get("retired").toString());
            }

            String body = MAPPER.writeValueAsString(drugData);
            openmrsRestClient.createOrUpdate(RESOURCE, uuid, body);

            if (!isRetired && medication.getStatus() == MedicationStatus.INACTIVE) {
                log.info("Retiring existing drug in OpenMRS with uuid {}", uuid);
                openmrsRestClient.delete(RESOURCE, uuid);
            } else if (isRetired && medication.getStatus() == MedicationStatus.ACTIVE) {
                log.info("Restoring existing drug in OpenMRS with uuid {}", uuid);
                openmrsRestClient.createOrUpdate(RESOURCE, uuid, MAPPER.writeValueAsString(Map.of("deleted", "false")));
            }
        }
    }
}
