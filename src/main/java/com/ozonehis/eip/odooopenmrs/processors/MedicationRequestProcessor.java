/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.processors;

import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.handlers.PartnerHandler;
import com.ozonehis.eip.odooopenmrs.handlers.SaleOrderLineHandler;
import com.ozonehis.eip.odooopenmrs.handlers.SalesOrderHandler;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
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
    private PartnerMapper partnerMapper;

    @Autowired
    private SalesOrderHandler salesOrderHandler;

    @Autowired
    private PartnerHandler partnerHandler;

    @Autowired
    private SaleOrderLineHandler saleOrderLineHandler;

    @Autowired
    private OdooClient odooClient;

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
                    var partner = partnerMapper.toOdoo(patient);
                    // If the MedicationRequest is canceled, remove the item from the quotation
                    if (medicationRequest.getStatus().equals(MedicationRequest.MedicationRequestStatus.CANCELLED)) {
                        handleSaleOrderWithItems(encounterVisitUuid, medicationRequest, exchange, producerTemplate);
                    } else {
                        SaleOrder saleOrder = salesOrderHandler.getSalesOrder(encounterVisitUuid);
                        //                        SaleOrder saleOrder = null;
                        if (saleOrder != null) {
                            // If the sale order exists, update it
                            Integer saleOrderLineId = saleOrderLineHandler.createSaleOrderLineIfProductExists(
                                    medicationRequest, saleOrder);
                            if (saleOrderLineId != null) {
                                List<Integer> saleOrderLineIdList = new ArrayList<>();
                                saleOrderLineIdList.add(saleOrderLineId);
                                saleOrder.setOrderLine(saleOrderLineIdList);
                            }
                            log.info("TESTING: IN MEDICATION REQUEST saleOrder != null {}", saleOrder);
                            salesOrderHandler.sendSalesOrder(
                                    producerTemplate, "direct:odoo-update-sales-order-route", saleOrder);
                        } else {
                            // If the sale order does not exist, create it
                            SaleOrder newSaleOrder = saleOrderMapper.toOdoo(encounter);
                            newSaleOrder.setOrderPartnerId(partnerHandler.partnerExists(patient.getIdPart()));
                            Object[] records = (Object[]) odooClient.execute(
                                    com.ozonehis.eip.odooopenmrs.Constants.CREATE_METHOD,
                                    com.ozonehis.eip.odooopenmrs.Constants.SALE_ORDER_MODEL,
                                    List.of(OdooUtils.convertObjectToMap(newSaleOrder)),
                                    null);

                            newSaleOrder.setOrderId((Integer) records[0]);
                            Integer saleOrderLineId = saleOrderLineHandler.createSaleOrderLineIfProductExists(
                                    medicationRequest, newSaleOrder);
                            if (saleOrderLineId != null) {
                                List<Integer> saleOrderLineIdList = new ArrayList<>();
                                saleOrderLineIdList.add(saleOrderLineId);
                                newSaleOrder.setOrderLine(saleOrderLineIdList);
                            }
                            log.info("TESTING: IN MEDICATION REQUEST saleOrder == null {}", newSaleOrder);
                            //                            salesOrderHandler.sendSalesOrder(
                            //                                    producerTemplate,
                            // "direct:odoo-create-sales-order-route", newSaleOrder);
                        }
                    }
                } else if ("d".equals(eventType)) {
                    handleSaleOrderWithItems(encounterVisitUuid, medicationRequest, exchange, producerTemplate);
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing MedicationRequest", exchange, e);
        }
    }

    private void handleSaleOrderWithItems(
            String encounterVisitUuid,
            MedicationRequest medicationRequest,
            Exchange exchange,
            ProducerTemplate producerTemplate) {
        SaleOrder saleOrder = salesOrderHandler.getSalesOrder(encounterVisitUuid);
        if (saleOrder != null) {
            log.debug("Removing item from sale order with ID {}", medicationRequest.getIdPart());
            //            saleOrder.removeSaleOrderLine(medicationRequest.getIdPart());
            //
            //            String route = saleOrder.hasSaleOrderLines()
            //                    ? "direct:odoo-update-sales-order-route"
            //                    : "direct:odoo-delete-sales-order-route";
            //            salesOrderHandler.sendSalesOrder(producerTemplate, route, saleOrder);
        } else {
            log.debug("Sale order with ID {} already deleted", encounterVisitUuid);
            exchange.getMessage().setHeader(com.ozonehis.eip.odooopenmrs.Constants.HEADER_EVENT_PROCESSED, true);
        }
    }
}
