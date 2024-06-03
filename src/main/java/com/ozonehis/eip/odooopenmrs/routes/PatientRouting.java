/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.processors.PatientProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Setter
@Component
public class PatientRouting extends RouteBuilder {

    @Value("${odoo.openmrs.enable.patient.sync}")
    private boolean isPatientSyncEnabled;

    @Autowired
    private PatientProcessor patientProcessor;

    Predicate isPatientSyncEnabled() {
        return exchange -> isPatientSyncEnabled
                || Boolean.TRUE.equals(exchange.getIn().getHeader(Constants.HEADER_ENABLE_PATIENT_SYNC, Boolean.class));
    }

    @Override
    public void configure() {
        // spotless:off
        from("direct:patient-to-partner-router")
                .routeId("patient-to-partner-router")
                .filter(isPatientSyncEnabled())
                .log(LoggingLevel.INFO, "Patient sync is enabled")
                .process(patientProcessor)
                .end();

        from("direct:fhir-patient")
                .routeId("fhir-patient-to-partner-router")
                .to("direct:patient-to-partner-router")
                .end();
        // spotless:on
    }
}
