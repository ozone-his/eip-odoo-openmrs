/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.odoo.openmrs.client.OdooFhirClient;
import com.ozonehis.eip.odoo.openmrs.client.OpenmrsRestClient;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Medication.MedicationStatus;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class ProductSynchronizer {

    protected static final String RESOURCE = "drug";

    private static ObjectMapper MAPPER = new ObjectMapper();

    private OdooFhirClient odooFhirClient;

    private IGenericClient openmrsFhirClient;

    private OpenmrsRestClient openmrsRestClient;

    public ProductSynchronizer(
            OdooFhirClient odooFhirClient, IGenericClient openmrsFhirClient, OpenmrsRestClient openmrsRestClient) {
        this.odooFhirClient = odooFhirClient;
        this.openmrsFhirClient = openmrsFhirClient;
        this.openmrsRestClient = openmrsRestClient;
    }

    @Scheduled(initialDelayString = "${eip.product.sync.initial.delay}", fixedDelayString = "${eip.product.sync.delay}")
    public void syncProducts() throws Exception {
        Bundle bundle = odooFhirClient.getAll(Medication.class);
        log.info("Read {} drugs from Odoo to sync", bundle.getEntry().size());

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            Medication medication = (Medication) entry.getResource();
            final String id = medication.getIdElement().getIdPart();
            String uuid = null;
            byte[] drug = openmrsRestClient.get(RESOURCE, id);
            if (drug == null && medication.getStatus() == MedicationStatus.INACTIVE) {
                log.info("Skipping new inactive drug");
                continue;
            }

            boolean isRetired = false;
            boolean exists = false;
            if (drug == null) {
                if (medication.getCode().getCoding().isEmpty()) {
                    log.warn("Skipping new drug with external id {} because of missing concept mapping", id);
                    continue;
                }

                log.info("Creating new drug in OpenMRS with uuid {}", id);
            } else {
                uuid = id;
                exists = true;
                if (log.isDebugEnabled()) {
                    log.debug("Updating existing drug in OpenMRS with uuid {}", uuid);
                }

                Map<String, Object> drugMap = MAPPER.readValue(drug, Map.class);
                isRetired = Boolean.valueOf(drugMap.get("retired").toString());
            }

            MethodOutcome outcome =
                    openmrsFhirClient.update().resource(medication).execute();
            if (outcome.getOperationOutcome() != null) {
                OperationOutcome ou = (OperationOutcome) outcome.getOperationOutcome();
                if (!ou.getIssue().isEmpty()) {
                    final String op = exists ? "updating" : "creating";
                    log.error(
                            "An issue has been encountered while {} the resource with uuid {} in OpenMRS, ",
                            op,
                            uuid,
                            ou.getIssue().get(0).getDiagnostics());
                }
            }

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
