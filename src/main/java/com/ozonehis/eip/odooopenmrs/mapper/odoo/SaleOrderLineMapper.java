/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import com.ozonehis.eip.odooopenmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
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
            if (serviceRequest.hasCode()) {
                saleOrderLine.setSaleOrderLineName(
                        serviceRequest.getCode().getCodingFirstRep().getCode());
            }
            saleOrderLine.setSaleOrderLineProductUomQty(1.0f); // default quantity is 1 for serviceRequests.
            String requesterDisplay = serviceRequest.getRequester().getDisplay();
            String serviceDisplay = serviceRequest.getCode().getText();
            saleOrderLine.setSaleOrderLineName(serviceDisplay + " | Requester: " + requesterDisplay);

        } else if (resource instanceof MedicationRequest medicationRequest) {
            if (medicationRequest.hasDispenseRequest()) {
                if (medicationRequest.getDispenseRequest().hasQuantity()) {
                    Quantity quantity = medicationRequest.getDispenseRequest().getQuantity();
                    saleOrderLine.setSaleOrderLineProductUomQty(
                            quantity.getValue().floatValue());
                    saleOrderLine.setSaleOrderLineProductUom(quantity.getCode());
                }
            }

            if (medicationRequest.hasMedicationReference()) {
                if (medicationRequest.getMedicationReference().hasReference()) {
                    String medicationCode = medicationRequest
                            .getMedicationReference()
                            .getReference()
                            .split("/")[1];
                    saleOrderLine.setSaleOrderLineName(medicationCode);
                }
            }

            String requesterDisplay = medicationRequest.getRequester().getDisplay();
            String medicationDisplay =
                    medicationRequest.getMedicationReference().getDisplay();
            saleOrderLine.setSaleOrderLineName(saleOrderLine.getSaleOrderLineName() + " | " + medicationDisplay
                    + " | Requester: " + requesterDisplay);
            // Add dosage instructions to the notes.
            saleOrderLine.setSaleOrderLineName(saleOrderLine.getSaleOrderLineName() + " | Notes: "
                    + constructDosageInstructionsText(medicationRequest));

        } else {
            throw new IllegalArgumentException("Sales Order Mapper Unsupported resource type: "
                    + resource.getClass().getName());
        }
        return saleOrderLine;
    }

    /**
     * Construct dosage instructions text from the medication request. Format: DOSE 10 tablet — oral — thrice daily — for 10
     * days — REFILLS 1 — QUANTITY 30 Tablet
     *
     * @param medicationRequest medication request
     * @return dosage instructions text
     */
    protected String constructDosageInstructionsText(MedicationRequest medicationRequest) {
        Dosage dosage = medicationRequest.getDosageInstructionFirstRep();
        StringBuilder dosageInstructions = new StringBuilder();

        if (dosage.hasDoseAndRate()) {
            dosageInstructions.append("DOSE ");
            dosageInstructions.append(
                    dosage.getDoseAndRateFirstRep().getDoseQuantity().getValue());
            dosageInstructions.append(" ");
            dosageInstructions.append(
                    dosage.getDoseAndRateFirstRep().getDoseQuantity().getUnit());
        }

        if (dosage.hasRoute()) {
            dosageInstructions.append(" — ");
            dosageInstructions.append(dosage.getRoute().getText());
        }

        if (dosage.hasTiming()) {
            dosageInstructions.append(" — ");
            dosageInstructions.append(dosage.getTiming().getCode().getText());
            dosageInstructions.append(" — for ");
            dosageInstructions.append(dosage.getTiming().getRepeat().getDuration());
            dosageInstructions.append(" ");
            dosageInstructions.append(
                    dosage.getTiming().getRepeat().getDurationUnit().getDisplay());
        }
        if (medicationRequest.hasDispenseRequest()) {
            dosageInstructions.append(" — REFILLS ");
            dosageInstructions.append(medicationRequest.getDispenseRequest().getNumberOfRepeatsAllowed());
            dosageInstructions.append(" - QUANTITY ");
            dosageInstructions.append(
                    medicationRequest.getDispenseRequest().getQuantity().getValue());
            dosageInstructions.append(" ");
            dosageInstructions.append(
                    medicationRequest.getDispenseRequest().getQuantity().getUnit());
        }
        return dosageInstructions.toString();
    }
}
