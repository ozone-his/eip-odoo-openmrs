/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
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
        try {
            Message message = exchange.getMessage();
            Patient patient = message.getBody(Patient.class);
            Partner partner = mapper.toOdoo(patient);

            if (patient == null || partner == null) {
                return;
            }

            String eventType = message.getHeader(HEADER_FHIR_EVENT_TYPE, String.class);
            Partner fetchedPartner = partnerHandler.getPartnerByID(partner.getPartnerRef());
            Map<String, Object> headers = new HashMap<>();
            if (fetchedPartner != null) {
                partner.setPartnerId(fetchedPartner.getPartnerId());
                headers.put(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(partner.getPartnerId()));

                if ("c".equals(eventType) || "u".equals(eventType)) {
                    headers.put(HEADER_FHIR_EVENT_TYPE, "u");
                } else {
                    headers.put(HEADER_FHIR_EVENT_TYPE, "d");
                }
            } else {
                // Partner does not yet exist in Odoo. The inbound event type may be "u"
                // (e.g. Debezium captured an OpenMRS patient.date_changed UPDATE that is
                // the patient's first sync to Odoo). Forcing "c" here so the route
                // dispatches to odoo-create-partner-route; otherwise the message is
                // silently no-op'd by odoo-update-partner-route on a non-existent
                // partner, and the new patient never reaches Odoo.
                if ("d".equals(eventType)) {
                    headers.put(HEADER_FHIR_EVENT_TYPE, "d");
                } else {
                    headers.put(HEADER_FHIR_EVENT_TYPE, "c");
                }
            }
            exchange.getMessage().setHeaders(headers);
            exchange.getMessage().setBody(partner);
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Patient", exchange, e);
        }
    }
}
