/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import lombok.Getter;
import lombok.Setter;
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

    @Override
    public void process(Exchange exchange) {
        Message message = exchange.getMessage();
        Patient patient = message.getBody(Patient.class);
        Partner partner = mapper.toOdoo(patient);

        var headers = message.getHeaders();
        headers.put(Constants.HEADER_ODOO_RESOURCE, partner);
        headers.put(Constants.HEADER_ODOO_DOCTYPE, "partner");

        String eventType = message.getHeader(HEADER_FHIR_EVENT_TYPE, String.class);
        if ("u".equals(eventType) || "d".equals(eventType)) {
            headers.put(Constants.HEADER_ODOO_ID, partner.getPartnerRef());
        }

        exchange.getMessage().setHeaders(headers);
        exchange.getMessage().setBody(partner);
    }
}
