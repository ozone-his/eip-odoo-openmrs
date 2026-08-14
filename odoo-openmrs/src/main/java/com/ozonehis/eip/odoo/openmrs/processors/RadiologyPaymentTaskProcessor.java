/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.TaskHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Watches active radiology ServiceRequests and tracks their Odoo order
 * status as a FHIR Task linked via basedOn - decoupling downstream
 * consumers (like the Orthanc bridge) from any direct knowledge of Odoo.
 *
 * Task status mapping (constrained by which FHIR Task statuses this
 * OpenMRS version's FHIR2 module can actually persist - confirmed via live
 * testing that only requested/accepted/rejected/completed work; several
 * others, including entered-in-error and failed, fail server-side with a
 * HAPI-0389 error regardless of client-side code):
 *  - requested: Task just created, or a payment check couldn't be
 *    completed (Odoo unreachable, etc.) - treated the same as "not yet
 *    confirmed" rather than as a distinct error state, since this
 *    OpenMRS version cannot persist entered-in-error/failed at all
 *  - accepted: Odoo payment confirmed
 *  - rejected: the Odoo sale order itself was cancelled (state=cancel)
 *
 * This mirrors the payment-check logic previously implemented directly
 * inside the Orthanc bridge's own OdooPaymentGate, moved here since this
 * bridge already owns the OpenMRS<->Odoo relationship, using this
 * project's own established OdooClient (XML-RPC).
 *
 * Fetches the ServiceRequest bundle directly via IGenericClient (matching
 * TaskHandler's approach) rather than this project's openmrs-fhir Camel
 * component, since that component is designed for single-resource-by-ID
 * lookups. Filters by status client-side, since OpenMRS's FHIR2
 * ServiceRequest resource provider does not support the status/_count
 * search parameters (confirmed via a live HTTP 400).
 */
@Slf4j
@Setter
@Component
public class RadiologyPaymentTaskProcessor implements Processor {

    private enum OdooOrderState {
        PENDING,
        CONFIRMED,
        REJECTED
    }

    @Autowired
    private IGenericClient openmrsFhirClient;

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private TaskHandler taskHandler;

    // Radiology concept UUIDs - same whitelist used by the Orthanc bridge's
    // own worklist-creation logic, kept in sync manually across both repos
    // for now.
    private static final Set<String> RADIOLOGY_CONCEPT_UUIDS = new HashSet<>(Arrays.asList(
            "e3dea2c8-62c6-4487-bdaa-1d009642f7ad", // RX01 - Chest X-ray
            "82e7d36c-078d-40c6-9854-92b376099307", // RX02 - Abdominal X-ray
            "701257a2-885e-4249-8319-d9597d2970af", // RX03 - Bone X-ray
            "b25dcc00-800f-48ac-b31a-f1e9cc53d787", // RX04 - Intravenous urography
            "81e0643c-a871-475e-8bd5-93945da8877d", // RX05 - Salpingo-urethrogram
            "1a5e3d73-f897-47ed-840b-d4537b7cc586", // RX06 - Barium enema
            "0a5ba175-fb7e-4d66-aa6a-ba058f3468c1", // RX07 - CT scan
            "d0b5d4a0-1001-0000-0000-000000000001",
            "d0b5d4a0-1002-0000-0000-000000000001",
            "d0b5d4a0-1003-0000-0000-000000000001",
            "d0b5d4a0-1004-0000-0000-000000000001",
            "d0b5d4a0-1005-0000-0000-000000000001",
            "d0b5d4a0-1006-0000-0000-000000000001",
            "d0b5d4a0-1007-0000-0000-000000000001",
            "d0b5d4a0-1008-0000-0000-000000000001"));

    // How far back each poll looks. Comfortably wider than the 30s poll interval, so a request is
    // still picked up if a cycle is slow or missed, without ever returning the whole table.
    private static final int LOOKBACK_MINUTES = 30;

    @Override
    public void process(Exchange exchange) {
        // Bounded by _lastUpdated. The unfiltered search this replaces asked OpenMRS for EVERY
        // ServiceRequest on every 30s poll: on UAT that is 818 records and 1.5MB, and it takes
        // ~38 SECONDS to answer - so the HAPI client hit its socket read timeout and the
        // processor never completed a single cycle. Measured on the same server:
        //
        //     no filter                     37.7s   818 records
        //     _lastUpdated=gt<30 min ago>    0.10s     1 record
        //
        // Filtering on status would be the natural thing to do and is not possible: OpenMRS's
        // FHIR2 module rejects `status` as a search parameter with HTTP 400, which is why the
        // status check below stays client-side.
        String since = ZonedDateTime.now(ZoneOffset.UTC)
                .minusMinutes(LOOKBACK_MINUTES)
                .format(DateTimeFormatter.ISO_INSTANT);

        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(ServiceRequest.class)
                .where(new ca.uhn.fhir.rest.gclient.DateClientParam("_lastUpdated").after().second(since))
                .returnBundle(Bundle.class)
                .execute();

        if (bundle == null) {
            log.info("DEBUG: bundle is null");
            return;
        }
        log.info("DEBUG: bundle entries = {}", bundle.getEntry().size());

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (!(entry.getResource() instanceof ServiceRequest)) {
                continue;
            }
            ServiceRequest serviceRequest = (ServiceRequest) entry.getResource();
            String serviceRequestId = serviceRequest.getIdElement().getIdPart();
            log.info("DEBUG: SR {} status={}", serviceRequestId, serviceRequest.getStatus());

            if (serviceRequest.getStatus() != ServiceRequest.ServiceRequestStatus.ACTIVE) {
                continue;
            }

            log.info("DEBUG: SR {} isRadiologyOrder={}", serviceRequestId, isRadiologyOrder(serviceRequest));
            if (!isRadiologyOrder(serviceRequest)) {
                continue;
            }

            Task existingTask = taskHandler.getTaskByServiceRequestId(serviceRequestId);
            log.info("DEBUG: SR {} existingTask status={}", serviceRequestId,
                    existingTask == null ? "null" : existingTask.getStatus());
            if (existingTask != null
                    && (existingTask.getStatus() == Task.TaskStatus.ACCEPTED
                            || existingTask.getStatus() == Task.TaskStatus.REJECTED)) {
                // Already reached a final state - nothing further to do.
                continue;
            }

            if (existingTask == null) {
                existingTask = taskHandler.createTask(serviceRequestId, Task.TaskStatus.REQUESTED);
            }

            String patientUuid = serviceRequest.getSubject().getReferenceElement().getIdPart();
            String procedureDesc =
                    serviceRequest.getCode().getText() != null ? serviceRequest.getCode().getText() : "";

            processOrderStateCheck(patientUuid, procedureDesc, existingTask);
        }
    }

    private boolean isRadiologyOrder(ServiceRequest serviceRequest) {
        return serviceRequest.getCode().getCoding().stream()
                .anyMatch(coding -> RADIOLOGY_CONCEPT_UUIDS.contains(coding.getCode()));
    }

    private void processOrderStateCheck(String patientUuid, String procedureDesc, Task existingTask) {
        try {
            OdooOrderState state = checkOdooOrderState(patientUuid, procedureDesc);
            switch (state) {
                case CONFIRMED:
                    taskHandler.updateTaskStatus(existingTask, Task.TaskStatus.ACCEPTED);
                    break;
                case REJECTED:
                    taskHandler.updateTaskStatus(existingTask, Task.TaskStatus.REJECTED);
                    break;
                case PENDING:
                default:
                    // Stays at requested - re-check on next poll.
                    break;
            }
        } catch (Exception e) {
            // A genuine check failure - treated the same as "not yet
            // confirmed" (stays at requested), since this OpenMRS version
            // cannot persist a distinct error status for Task.
            log.warn(
                    "Payment check failed for patient {} procedure '{}': {} (cause: {})",
                    patientUuid,
                    procedureDesc,
                    e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "none",
                    e);
        }
    }

    /**
     * Checks the Odoo sale order's own state (for rejection/cancellation)
     * and, if not rejected, its payment status. Logic ported from the
     * Orthanc bridge's original OdooPaymentGate, adapted to this project's
     * established OdooClient (XML-RPC), and extended to also check the
     * order's own cancellation state.
     */
    private OdooOrderState checkOdooOrderState(String patientUuid, String procedureDesc) throws Exception {
        List<Object> lineCriteria =
                Arrays.asList(Arrays.asList("order_id.partner_id.ref", "=", patientUuid));
        Object[] lines = odooClient.searchAndRead(
                Constants.SALE_ORDER_LINE_MODEL,
                lineCriteria,
                Arrays.asList("id", "name", "qty_invoiced", "order_id"));

        log.info("DEBUG: lines found = {}", lines == null ? "null" : lines.length);
        if (lines == null || lines.length == 0) {
            log.debug("No sale order lines for patient {} - not yet confirmed", patientUuid);
            return OdooOrderState.PENDING;
        }

        String matchKey = procedureDesc != null && procedureDesc.length() > 6
                ? procedureDesc.substring(0, 6).toLowerCase()
                : (procedureDesc != null ? procedureDesc.toLowerCase() : "");

        java.util.Map<?, ?> mostRecentLine = null;
        for (Object lineObj : lines) {
            java.util.Map<?, ?> line = (java.util.Map<?, ?>) lineObj;
            String lineName = String.valueOf(line.get("name")).toLowerCase();
            if (!matchKey.isEmpty() && lineName.contains(matchKey)) {
                mostRecentLine = line;
                break;
            }
        }

        log.info("DEBUG: matchKey={} mostRecentLine={}", matchKey, mostRecentLine);
        if (mostRecentLine == null) {
            log.debug("Procedure '{}' not found among sale order lines for patient {} - not yet confirmed",
                    procedureDesc, patientUuid);
            return OdooOrderState.PENDING;
        }

        Object orderIdField = mostRecentLine.get("order_id");
        Integer orderId = null;
        String orderName = "";
        if (orderIdField instanceof Object[] && ((Object[]) orderIdField).length > 1) {
            Object idObj = ((Object[]) orderIdField)[0];
            orderId = idObj instanceof Number ? ((Number) idObj).intValue() : null;
            orderName = String.valueOf(((Object[]) orderIdField)[1]);
        }
        if (orderId == null) {
            throw new IllegalStateException("Could not resolve order id for sale order line");
        }

        // Check the sale order's own state first - a cancelled order is
        // rejected regardless of any prior payment activity.
        List<Object> orderCriteria = Arrays.asList(Arrays.asList("id", "=", orderId));
        Object[] orders = odooClient.searchAndRead(
                "sale.order", orderCriteria, Arrays.asList("id", "state"));
        if (orders != null && orders.length > 0) {
            java.util.Map<?, ?> order = (java.util.Map<?, ?>) orders[0];
            String orderState = String.valueOf(order.get("state"));
            if ("cancel".equals(orderState)) {
                log.info("Sale order {} is cancelled - rejecting", orderName);
                return OdooOrderState.REJECTED;
            }
        }

        Object qtyInvoiced = mostRecentLine.get("qty_invoiced");
        double qty = qtyInvoiced instanceof Number ? ((Number) qtyInvoiced).doubleValue() : 0;
        if (qty <= 0) {
            return OdooOrderState.PENDING;
        }

        if (orderName.isEmpty()) {
            throw new IllegalStateException("Could not resolve order name for sale order line");
        }

        List<Object> invoiceCriteria = Arrays.asList(Arrays.asList("invoice_origin", "=", orderName));
        Object[] invoices = odooClient.searchAndRead(
                Constants.ACCOUNT_MOVE_MODEL,
                invoiceCriteria,
                Arrays.asList("name", "state", "payment_state", "amount_residual"));

        log.info("DEBUG: invoices found = {}", invoices == null ? "null" : invoices.length);
        if (invoices == null || invoices.length == 0) {
            return OdooOrderState.PENDING;
        }

        for (Object invoiceObj : invoices) {
            java.util.Map<?, ?> invoice = (java.util.Map<?, ?>) invoiceObj;
            String paymentState = String.valueOf(invoice.get("payment_state"));
            Object residualObj = invoice.get("amount_residual");
            double residual = residualObj instanceof Number ? ((Number) residualObj).doubleValue() : -1;
            if ("paid".equals(paymentState) && residual == 0.0) {
                return OdooOrderState.CONFIRMED;
            }
        }

        return OdooOrderState.PENDING;
    }
}
