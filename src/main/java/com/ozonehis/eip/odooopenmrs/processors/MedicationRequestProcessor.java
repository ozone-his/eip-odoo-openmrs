/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.processors;

import com.ozonehis.eip.odooopenmrs.handlers.PartnerHandler;
import com.ozonehis.eip.odooopenmrs.handlers.SaleOrderLineHandler;
import com.ozonehis.eip.odooopenmrs.handlers.SalesOrderHandler;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.*;
import org.openmrs.eip.fhir.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class MedicationRequestProcessor implements Processor {

    @Autowired
    private SaleOrderMapper saleOrderMapper;

    @Autowired
    private SalesOrderHandler salesOrderHandler;

    @Autowired
    private PartnerHandler partnerHandler;

    @Autowired
    private SaleOrderLineHandler saleOrderLineHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

            Patient patient = null;
            Encounter encounter = null;
            MedicationRequest medicationRequest = null;
            Medication medication = null;

            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof MedicationRequest) {
                    medicationRequest = (MedicationRequest) resource;
                } else if (resource instanceof Medication) {
                    medication = (Medication) resource;
                }
            }

            if (patient == null || encounter == null || medicationRequest == null || medication == null) {
                throw new CamelExecutionException(
                        "Invalid Bundle. Bundle must contain Patient, Encounter, MedicationRequest and Medication",
                        exchange);
            } else {
                log.debug("Processing MedicationRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage().getHeader(Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                String encounterVisitUuid = encounter.getPartOf().getReference().split("/")[1];
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    partnerHandler.ensurePartnerExistsAndUpdate(producerTemplate, patient);
                    if (medicationRequest.getStatus().equals(MedicationRequest.MedicationRequestStatus.CANCELLED)) {
                        // TODO: Handle sale order with item, maybe mark as cancelled
                    } else {
                        SaleOrder saleOrder = salesOrderHandler.getSalesOrderIfExists(encounterVisitUuid);
                        if (saleOrder != null) {
                            // If sale order exists create sale order line and link it to sale order
                            int saleOrderLineId = saleOrderLineHandler.createSaleOrderLineIfProductExists(
                                    medicationRequest, saleOrder);
                            List<Integer> saleOrderLineIdList = new ArrayList<>();
                            saleOrderLineIdList.add(saleOrderLineId);
                            saleOrder.setOrderLine(saleOrderLineIdList);
                            log.info(
                                    "MedicationRequestProcessor: Created sale order line with id {} and linked to sale order {}",
                                    saleOrderLineId,
                                    saleOrder);

                            salesOrderHandler.sendSalesOrder(
                                    producerTemplate, "direct:odoo-update-sales-order-route", saleOrder);
                        } else {
                            // If the sale order does not exist, create it, then create sale order line and link it to
                            // sale order
                            SaleOrder newSaleOrder = saleOrderMapper.toOdoo(encounter);
                            newSaleOrder.setOrderPartnerId(partnerHandler.partnerExists(patient.getIdPart()));
                            int newSaleOrderId = salesOrderHandler.createSaleOrder(newSaleOrder);
                            log.info("MedicationRequestProcessor: Created sale order with id {}", newSaleOrderId);
                            newSaleOrder.setOrderId(newSaleOrderId);

                            int saleOrderLineId = saleOrderLineHandler.createSaleOrderLineIfProductExists(
                                    medicationRequest, newSaleOrder);
                            List<Integer> saleOrderLineIdList = new ArrayList<>();
                            saleOrderLineIdList.add(saleOrderLineId);
                            newSaleOrder.setOrderLine(saleOrderLineIdList);

                            log.info(
                                    "MedicationRequestProcessor: Created sale order {} and sale order line {} and linked to sale order",
                                    newSaleOrderId,
                                    saleOrderLineId);
                        }
                    }
                } else if ("d".equals(eventType)) {
                    // TODO: Handle sale order with item, when event type is delete
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing MedicationRequest", exchange, e);
        }
    }
}
