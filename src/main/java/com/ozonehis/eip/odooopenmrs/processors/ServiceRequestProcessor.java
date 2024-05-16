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
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
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

import java.io.IOException;
import java.util.List;

@Slf4j
@Setter
@Component
public class ServiceRequestProcessor implements Processor {

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

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

            Patient patient = null;
            Encounter encounter = null;
            ServiceRequest serviceRequest = null;
            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof ServiceRequest) {
                    serviceRequest = (ServiceRequest) resource;
                }
            }

            if (patient == null || encounter == null || serviceRequest == null) {
                throw new IllegalArgumentException("Patient, Encounter or ServiceRequest not found in the bundle");
            } else {
                if (serviceRequest.getStatus().equals(ServiceRequest.ServiceRequestStatus.ACTIVE)
                        && serviceRequest.getIntent().equals(ServiceRequest.ServiceRequestIntent.ORDER)) {
                    log.info("Processing ServiceRequest for patient with UUID {}", patient.getIdPart());
                    var partner = partnerMapper.toOdoo(patient);
                    String eventType = exchange.getMessage().getHeader(Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                    if (eventType == null) {
                        throw new IllegalArgumentException("Event type not found in the exchange headers");
                    }
                    if ("c".equals(eventType) || "u".equals(eventType)) {
                        partnerHandler.ensurePartnerExistsAndUpdate(producerTemplate, patient);
                        String encounterVisitUuid =
                                encounter.getPartOf().getReference().split("/")[1];
                        SaleOrder saleOrder = salesOrderHandler.getSalesOrder(encounterVisitUuid);
                        if (saleOrder != null) {
                            // Sale order exists, update it with the new sale order line
                            SaleOrder finalSaleOrder = saleOrder;
                            this.saleOrderLineHandler
                                    .createSaleOrderLineIfItemExists(serviceRequest)
                                    .ifPresent(quotationItem -> {
                                        if (finalSaleOrder.hasSaleOrderLine(quotationItem)) {
                                            log.debug("Sale order line already exists. Already processed skipping...");
                                        } else {
                                            finalSaleOrder.addSaleOrderLine(quotationItem);
                                        }
                                    });
                            salesOrderHandler.sendSalesOrder(
                                    producerTemplate, "direct:odoo-update-sales-order-route", finalSaleOrder);
                        } else {
                            saleOrder = saleOrderMapper.toOdoo(encounter);
                            saleOrder.setOrderTitle(partner.getPartnerName());
                            saleOrder.setOrderPartyName(partner.getPartnerRef());
                            saleOrder.setOrderPartnerName(partner.getPartnerName());
                            this.saleOrderLineHandler
                                    .createSaleOrderLineIfItemExists(serviceRequest)
                                    .ifPresent(saleOrder::addSaleOrderLine);
                            salesOrderHandler.sendSalesOrder(
                                    producerTemplate, "direct:odoo-create-sales-order-route", saleOrder);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new CamelExecutionException("Error processing ServiceRequest", exchange, e);
        }
    }
}
