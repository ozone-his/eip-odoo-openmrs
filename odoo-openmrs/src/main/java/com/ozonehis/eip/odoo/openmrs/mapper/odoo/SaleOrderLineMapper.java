/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper.odoo;

import com.ozonehis.eip.odoo.openmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SaleOrderLineMapper<R extends Resource> implements ToOdooMapping<R, SaleOrderLine> {

    @Override
    public SaleOrderLine toOdoo(R resource) {
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        if (resource instanceof ServiceRequest serviceRequest) {
            saleOrderLine.setSaleOrderLineProductUomQty(1.0f); // default quantity is 1 for serviceRequests.
            String requesterDisplay = serviceRequest.getRequester().getDisplay();
            String serviceDisplay = serviceRequest.getCode().getText();
            saleOrderLine.setSaleOrderLineName(serviceDisplay + " | Orderer: " + requesterDisplay);

        } else if (resource instanceof MedicationRequest medicationRequest) {
            if (medicationRequest.hasDispenseRequest()) {
                if (medicationRequest.getDispenseRequest().hasQuantity()) {
                    Quantity quantity = medicationRequest.getDispenseRequest().getQuantity();
                    saleOrderLine.setSaleOrderLineProductUomQty(
                            quantity.getValue().floatValue());
                    saleOrderLine.setSaleOrderLineProductUom(quantity.getCode());
                }
            }

            String requesterDisplay = medicationRequest.getRequester().getDisplay();
            String medicationDisplay =
                    medicationRequest.getMedicationReference().getDisplay();
            saleOrderLine.setSaleOrderLineName(
                    medicationDisplay + " | " + constructDosageInstructionsText(medicationRequest, saleOrderLine)
                            + " | Orderer: " + requesterDisplay);

        } else {
            throw new IllegalArgumentException("Sale Order Mapper Unsupported resource type: "
                    + resource.getClass().getName());
        }
        return saleOrderLine;
    }

    protected String constructDosageInstructionsText(MedicationRequest medicationRequest, SaleOrderLine saleOrderLine) {
        Dosage dosage = medicationRequest.getDosageInstructionFirstRep();
        StringBuilder dosageInstructions = new StringBuilder();

        if (dosage.hasDoseAndRate()) {
            if (saleOrderLine.getSaleOrderLineProductUomQty() != null) {
                dosageInstructions.append(saleOrderLine.getSaleOrderLineProductUomQty());
                dosageInstructions.append(" ");
                dosageInstructions.append(
                        dosage.getDoseAndRateFirstRep().getDoseQuantity().getUnit());
                dosageInstructions.append(" | ");
            }
            dosageInstructions.append(
                    dosage.getDoseAndRateFirstRep().getDoseQuantity().getValue());
            dosageInstructions.append(" ");
            dosageInstructions.append(
                    dosage.getDoseAndRateFirstRep().getDoseQuantity().getUnit());
        }

        if (dosage.hasRoute()) {
            dosageInstructions.append(" - ");
            dosageInstructions.append(dosage.getRoute().getText());
        }

        if (dosage.hasTiming()) {
            dosageInstructions.append(" - ");
            dosageInstructions.append(dosage.getTiming().getCode().getText());
            dosageInstructions.append(" - ");
            dosageInstructions.append(dosage.getTiming().getRepeat().getDuration());
            dosageInstructions.append(" ");
            dosageInstructions.append(
                    dosage.getTiming().getRepeat().getDurationUnit().getDisplay());
        }
        return dosageInstructions.toString();
    }
}
