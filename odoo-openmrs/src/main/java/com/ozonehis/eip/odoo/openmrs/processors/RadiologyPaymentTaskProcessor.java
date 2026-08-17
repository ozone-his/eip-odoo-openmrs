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
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.TaskHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import java.util.Date;
import java.time.format.DateTimeParseException;
import java.time.LocalDateTime;
import java.time.Instant;
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

    @Autowired
    private EncounterHandler encounterHandler;

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

    // How far back each poll looks.
    //
    // This must span the whole time an order can wait to be PAID, not just the poll interval. What
    // this processor is waiting for happens in Odoo - an invoice being settled - and that does not
    // touch the ServiceRequest, so the order's _lastUpdated never moves. An order therefore has to
    // stay inside the window from the moment it is placed until someone pays for it.
    //
    // An earlier version used 30 minutes, reasoning only about missed poll cycles. Measured on UAT:
    // an RX01 order placed at 09:45 and paid at 16:20 was never re-examined, so no Task was created
    // and the scan never reached the modality worklist. The order was simply outside the window by
    // the time the money arrived.
    //
    // Seven days is the practical ceiling on order-to-payment at UVL and matches the window
    // RadiologyOrderWorklistProcessor uses on the Orthanc side. The query stays cheap because it is
    // still bounded: the unfiltered search this replaces returned 818 records in 37.7s, a week's
    // worth returns a handful.
    private static final int LOOKBACK_DAYS = 7;

    @Override
    public void process(Exchange exchange) {
        // Bounded by _lastUpdated. The unfiltered search this replaces asked OpenMRS for EVERY
        // ServiceRequest on every 30s poll: on UAT that is 818 records and 1.5MB, and it takes
        // ~38 SECONDS to answer - so the HAPI client hit its socket read timeout and the
        // processor never completed a single cycle. Measured on the same server:
        //
        //     no filter                     37.7s   818 records
        //     _lastUpdated=gt<window>         0.10s     1 record
        //
        // Filtering on status would be the natural thing to do and is not possible: OpenMRS's
        // FHIR2 module rejects `status` as a search parameter with HTTP 400, which is why the
        // status check below stays client-side.
        String since = ZonedDateTime.now(ZoneOffset.UTC)
                .minusDays(LOOKBACK_DAYS)
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

            // When this order was placed. Used to reject sale order lines that predate it - see
            // checkOdooOrderState. Falls back to the resource's lastUpdated if authoredOn is
            // absent, and to null (meaning "cannot bound it") rather than to "now", because
            // defaulting to now would reject every legitimate line.
            Date authoredOn = serviceRequest.getAuthoredOn();
            if (authoredOn == null && serviceRequest.getMeta() != null) {
                authoredOn = serviceRequest.getMeta().getLastUpdated();
            }

            // The visit this order belongs to, which is what a payment has to share with it.
            // Null when it cannot be resolved - checkOdooOrderState then falls back to the older,
            // looser patient-wide match rather than refusing to gate at all.
            String visitUuid = resolveVisitUuid(serviceRequest);

            processOrderStateCheck(patientUuid, visitUuid, procedureDesc, authoredOn, existingTask);
        }
    }

    /**
     * Odoo returns datetimes as "yyyy-MM-dd HH:mm:ss" in UTC, with no zone marker and sometimes
     * with fractional seconds. Returns null rather than throwing when the value is missing or in
     * an unexpected shape, so the caller can decide what an unknown date means - here, to skip
     * the line rather than accept it.
     */
    private Instant parseOdooDate(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty() || "false".equals(raw)) {
            return null;
        }
        int dot = raw.indexOf('.');
        if (dot > 0) {
            raw = raw.substring(0, dot);
        }
        try {
            return LocalDateTime.parse(raw.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            log.warn("Unparseable Odoo create_date '{}'", value);
            return null;
        }
    }

    private boolean isRadiologyOrder(ServiceRequest serviceRequest) {
        return serviceRequest.getCode().getCoding().stream()
                .anyMatch(coding -> RADIOLOGY_CONCEPT_UUIDS.contains(coding.getCode()));
    }

    /**
     * The uuid of the visit an order belongs to, or null if it cannot be resolved.
     *
     * <p>Odoo groups a visit's charges into one sale order keyed on this uuid
     * (client_order_ref), so it is the link between an order and the money paid for it.
     */
    private String resolveVisitUuid(ServiceRequest serviceRequest) {
        try {
            if (serviceRequest.getEncounter() == null
                    || serviceRequest.getEncounter().getReference() == null) {
                return null;
            }
            String encounterId = serviceRequest.getEncounter().getReference().split("/")[1];
            org.hl7.fhir.r4.model.Encounter encounter = encounterHandler.getEncounterByEncounterID(encounterId);
            if (encounter == null || encounter.getPartOf() == null || encounter.getPartOf().getReference() == null) {
                return null;
            }
            return encounter.getPartOf().getReference().split("/")[1];
        } catch (Exception e) {
            log.warn("Could not resolve the visit for ServiceRequest {}: {}",
                    serviceRequest.getIdElement().getIdPart(), e.getMessage());
            return null;
        }
    }

    private void processOrderStateCheck(
            String patientUuid, String visitUuid, String procedureDesc, Date authoredOn, Task existingTask) {
        try {
            OdooOrderState state = checkOdooOrderState(patientUuid, visitUuid, procedureDesc, authoredOn);
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
    private OdooOrderState checkOdooOrderState(
            String patientUuid, String visitUuid, String procedureDesc, Date authoredOn) throws Exception {
        // Scoped to the order's OWN visit, not just its patient.
        //
        // Odoo groups a visit's charges into one sale order keyed on the visit uuid, so a payment
        // for this order can only be a line on that sale order. Searching by patient alone let a
        // payment made in one visit answer for an order placed in another: measured on UAT, paying
        // for a chest X-ray today admitted five chest X-rays ordered on 10-14 Aug for the same
        // patient, none of which had been paid for.
        //
        // Time bounding alone could not fix that. It rejects a line OLDER than the order, which is
        // why the 7 Aug invoice stopped authorising later scans, but says nothing about a NEWER
        // payment reaching backwards. Sharing a visit is the constraint that does.
        //
        // What this still cannot separate is two identical procedures within ONE visit: lines are
        // deduplicated per product, so both orders share a single line and paying once admits both.
        // Closing that needs a per-ServiceRequest link on the line, which Odoo does not carry today.
        List<Object> lineCriteria = visitUuid != null
                ? Arrays.asList(
                        Arrays.asList("order_id.partner_id.ref", "=", patientUuid),
                        Arrays.asList("order_id.client_order_ref", "=", visitUuid))
                : Arrays.asList(Arrays.asList("order_id.partner_id.ref", "=", patientUuid));
        if (visitUuid == null) {
            log.warn("No visit resolved for this order - falling back to a patient-wide payment match, "
                    + "which can admit a scan paid for in a different visit");
        }
        Object[] lines = odooClient.searchAndRead(
                Constants.SALE_ORDER_LINE_MODEL,
                lineCriteria,
                Arrays.asList("id", "name", "qty_invoiced", "order_id", "create_date"));

        log.info("DEBUG: lines found = {}", lines == null ? "null" : lines.length);
        if (lines == null || lines.length == 0) {
            log.debug("No sale order lines for patient {} - not yet confirmed", patientUuid);
            return OdooOrderState.PENDING;
        }

        String matchKey = procedureDesc != null && procedureDesc.length() > 6
                ? procedureDesc.substring(0, 6).toLowerCase()
                : (procedureDesc != null ? procedureDesc.toLowerCase() : "");

        // Only lines that could belong to THIS order, and genuinely the most recent of those.
        //
        // This used to take the FIRST line whose name matched the procedure, with no regard to
        // when it was created - despite the variable being called mostRecentLine. Since the
        // search is scoped only by patient, any older line for the same procedure answered for a
        // new order. Measured on UAT: three worklist entries were created on 10 Aug against a
        // single invoice raised on 7 Aug, worth 1, for orders that had no quotation of their own.
        // One paid scan therefore authorised unlimited later scans of the same type for that
        // patient, and unpaid imaging reached the modality.
        //
        // Odoo has no per-ServiceRequest link to key on: sale orders are grouped per visit
        // (client_order_ref = visit uuid) and lines deduplicated per product, so two orders for
        // the same procedure in one visit share a single line. Until a line carries the
        // ServiceRequest id, the tightest available bound is time: a line raised BEFORE the order
        // was authored cannot be payment for it.
        //
        // The 60s tolerance absorbs clock skew between OpenMRS and Odoo. It is deliberately small:
        // the line is written by this bridge seconds after the order, so a legitimate line is
        // never more than moments older than authoredOn.
        java.util.Map<?, ?> mostRecentLine = null;
        Instant cutoff = authoredOn == null ? null : authoredOn.toInstant().minusSeconds(60);
        Instant bestCreated = null;

        for (Object lineObj : lines) {
            java.util.Map<?, ?> line = (java.util.Map<?, ?>) lineObj;
            String lineName = String.valueOf(line.get("name")).toLowerCase();
            if (matchKey.isEmpty() || !lineName.contains(matchKey)) {
                continue;
            }

            Instant created = parseOdooDate(line.get("create_date"));
            if (cutoff != null) {
                if (created == null) {
                    log.warn("Sale order line {} has no parseable create_date - ignoring it rather than "
                            + "risk accepting an unrelated older line", line.get("id"));
                    continue;
                }
                if (created.isBefore(cutoff)) {
                    log.info("Ignoring sale order line {} created {} - predates order authored {}",
                            line.get("id"), created, authoredOn.toInstant());
                    continue;
                }
            }

            if (bestCreated == null || (created != null && created.isAfter(bestCreated))) {
                mostRecentLine = line;
                bestCreated = created;
            }
        }

        log.info("DEBUG: matchKey={} mostRecentLine={}", matchKey, mostRecentLine);
        if (mostRecentLine == null) {
            log.debug("No sale order line for procedure '{}' raised on or after this order was authored "
                    + "(patient {}) - not yet confirmed", procedureDesc, patientUuid);
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
