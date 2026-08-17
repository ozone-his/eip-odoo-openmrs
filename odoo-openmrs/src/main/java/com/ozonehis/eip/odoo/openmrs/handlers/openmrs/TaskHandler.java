/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Manages FHIR Task resources used to communicate order-level state (such as
 * payment confirmation) between EIP bridges, without either bridge needing
 * direct knowledge of the other system it's ultimately gating access to.
 *
 * A Task's basedOn reference links it to the ServiceRequest it concerns.
 * Task.status is used to represent three distinct outcomes of a payment
 * check:
 *  - no Task exists yet: payment not yet confirmed, still being polled
 *  - status = COMPLETED: payment confirmed, safe to act on
 *  - status = ENTEREDINERROR: the payment check itself failed (Odoo
 *    unreachable, auth failure, malformed data) - explicitly distinct from
 *    "not yet paid", so a consumer never mistakes a check failure for a
 *    legitimate pending state.
 */
@Slf4j
@Setter
@Component
public class TaskHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    /**
     * Finds the existing Task for a given ServiceRequest, if one exists.
     *
     * <p>The basedOn link is re-checked here rather than left to the server. OpenMRS's FHIR2 module
     * accepts {@code ?based-on=} and then ignores it - a query with an all-zero uuid returns every
     * Task in the system. Combined with findFirst(), that made this method answer "yes, a Task
     * already exists" for EVERY ServiceRequest as soon as one Task existed anywhere, so no order
     * after the first ever got a Task of its own and none of them could ever reach the modality
     * worklist. Measured on UAT: an RX01 order, paid in Odoo, was skipped with
     * "existingTask status=ACCEPTED" while the only Task in the system belonged to a different
     * patient's order.
     *
     * <p>The query parameter is left in place - harmless, and a real optimisation if OpenMRS ever
     * implements it - but the filter below is what makes the answer correct.
     */
    public Task getTaskByServiceRequestId(String serviceRequestId) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Task.class)
                .where(Task.BASED_ON.hasId(serviceRequestId))
                .returnBundle(Bundle.class)
                .execute();

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Task.class::isInstance)
                .map(Task.class::cast)
                .filter(task -> isBasedOn(task, serviceRequestId))
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether a Task's basedOn actually references the given ServiceRequest. References are compared
     * on the trailing id so "ServiceRequest/&lt;uuid&gt;", a bare uuid and an absolute URL all match.
     */
    private boolean isBasedOn(Task task, String serviceRequestId) {
        if (task.getBasedOn() == null) {
            return false;
        }
        return task.getBasedOn().stream()
                .map(org.hl7.fhir.r4.model.Reference::getReference)
                .filter(reference -> reference != null && !reference.isEmpty())
                .anyMatch(reference -> serviceRequestId.equals(reference.substring(reference.lastIndexOf('/') + 1)));
    }

    /**
     * Creates a new Task linked to the given ServiceRequest, with the given
     * status. Used the first time a payment check produces a definitive
     * result (either confirmed, or a genuine error) for an order that
     * doesn't have a Task yet.
     */
    public Task createTask(String serviceRequestId, Task.TaskStatus status) {
        Task task = new Task();
        task.setStatus(status);
        task.setIntent(Task.TaskIntent.ORDER);
        task.setBasedOn(java.util.List.of(new Reference("ServiceRequest/" + serviceRequestId)));

        Task created = (Task) openmrsFhirClient
                .create()
                .resource(task)
                .execute()
                .getResource();

        log.info("Created Task {} for ServiceRequest {} with status {}", created.getIdPart(), serviceRequestId, status);
        return created;
    }

    /**
     * Updates an existing Task's status - e.g. transitioning from a prior
     * ENTEREDINERROR state back to COMPLETED once a subsequent check
     * succeeds, or vice versa.
     */
    public void updateTaskStatus(Task task, Task.TaskStatus status) {
        task.setStatus(status);
        openmrsFhirClient.update().resource(task).execute();
        log.info("Updated Task {} to status {}", task.getIdPart(), status);
    }
}
