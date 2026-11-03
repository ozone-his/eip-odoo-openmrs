/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.ProductHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.PatientHandler;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import lombok.Setter;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Setter
public class EncounterProcessor implements Processor {

    @Value("${odoo.openmrs.enable.visit.sync}")
    private boolean isVisitSyncEnabled;

    @Autowired
    private EncounterHandler encounterHandler;

    @Autowired
    private PatientHandler patientHandler;

    @Autowired
    private PartnerHandler partnerHandler;

    @Autowired
    private ProductHandler productHandler;

    @Autowired
    private SaleOrderHandler saleOrderHandler;

    @Override
    public void process(Exchange exchange) throws Exception {
        Message message = exchange.getMessage();
        Encounter encounter = message.getBody(Encounter.class);

        // When enabled, As soon as a Visit is started in OpenMRS a sale order will be created in Odoo
        // and a sale order line with visit type will be added.
        if (isVisitSyncEnabled && encounterHandler.isVisit(encounter)) {
            if (productHandler.getProduct(encounter) == null) {
                throw new EIPException("No Odoo product mapped to visit type "
                        + encounterHandler.getVisitType(encounter).getCode());
            }
            String patientId = encounter.getSubject().getReferenceElement().getIdPart();
            if (patientId == null || patientId.isBlank()) {
                throw new EIPException("Visit has no patient reference: " + encounter.getIdPart());
            }
            Patient patient = patientHandler.getPatientByPatientID(patientId);
            try (ProducerTemplate producer = exchange.getContext().createProducerTemplate()) {
                Partner partner = partnerHandler.createOrUpdatePartner(producer, patient, null);
                SaleOrder order = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(encounter.getIdPart(), null);
                if (order == null) {
                    saleOrderHandler.createSaleOrderWithSaleOrderLine(
                            encounter, encounter, partner, encounter.getIdPart(), patientId, null, producer);
                } else {
                    saleOrderHandler.updateSaleOrderIfExistsWithSaleOrderLine(
                            encounter, order, encounter.getIdPart(), partner.getPartnerId(), patientId, null, producer);
                }
            }
        }
    }
}
