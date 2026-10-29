/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.VisitUtils;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.ProductHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.PatientHandler;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import lombok.Setter;
import org.apache.camel.Exchange;
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
public class VisitProcessor implements Processor {

    @Value("${odoo.openmrs.enable.visit.sync}")
    private boolean enableVisitSync;

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
        if (!enableVisitSync
                || !"c".equals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE, String.class))
                || !(exchange.getMessage().getBody() instanceof Encounter visit)
                || !VisitUtils.isVisit(visit)) {
            return;
        }
        if (productHandler.getProduct(visit) == null) {
            throw new EIPException("No Odoo product mapped to visit type "
                    + VisitUtils.getVisitType(visit).getCode());
        }
        Integer companyId = null;
        String patientId = visit.getSubject().getReferenceElement().getIdPart();
        if (patientId == null || patientId.isBlank()) {
            throw new EIPException("Visit has no patient reference: " + visit.getIdPart());
        }
        Patient patient = patientHandler.getPatientByPatientID(patientId);
        try (ProducerTemplate producer = exchange.getContext().createProducerTemplate()) {
            Partner partner = partnerHandler.createOrUpdatePartner(producer, patient, companyId);
            SaleOrder order = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(visit.getIdPart(), companyId);
            if (order == null) {
                saleOrderHandler.createSaleOrderWithSaleOrderLine(
                        visit, visit, partner, visit.getIdPart(), patientId, companyId, producer);
            } else {
                saleOrderHandler.updateSaleOrderIfExistsWithSaleOrderLine(
                        visit, order, visit.getIdPart(), partner.getPartnerId(), patientId, companyId, producer);
            }
        }
    }
}
