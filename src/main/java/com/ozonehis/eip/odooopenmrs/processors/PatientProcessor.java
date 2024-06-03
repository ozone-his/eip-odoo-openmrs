/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odooopenmrs.handlers.PartnerHandler;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
public class PatientProcessor implements Processor {

    @Autowired
    private PartnerMapper mapper;

    @Autowired
    private PartnerHandler partnerHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Message message = exchange.getMessage();
            Patient patient = message.getBody(Patient.class);
            Partner partner = mapper.toOdoo(patient);

            if (patient == null || partner == null) {
                return;
            }
            Partner fetchedPartner = partnerHandler.partnerExists(partner.getPartnerRef());
            if (fetchedPartner != null) {
                partner.setPartnerId(fetchedPartner.getPartnerId());
                partnerHandler.sendPartner(producerTemplate, "direct:odoo-update-partner-route", partner);
                return;
            }
            String eventType = message.getHeader(HEADER_FHIR_EVENT_TYPE, String.class);
            if ("c".equals(eventType)) {
                partnerHandler.sendPartner(producerTemplate, "direct:odoo-create-partner-route", partner);
            } else if ("u".equals(eventType)) {
                partnerHandler.sendPartner(producerTemplate, "direct:odoo-update-partner-route", partner);
            } else if ("d".equals(eventType)) {
                partnerHandler.sendPartner(producerTemplate, "direct:odoo-delete-partner-route", partner);
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Patient", exchange, e);
        }
    }
}
