/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.processors.MedicationRequestProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class MedicationRequestRouting extends RouteBuilder {

    private static final String MEDICATION_REQUEST_TO_SALE_ORDER_ROUTER = "medication-request-to-sale-order-router";

    private static final String MEDICATION_REQUEST_TO_SALE_ORDER_PROCESSOR =
            "medication-request-to-sale-order-processor";

    private static final String MEDICATION_REQUEST_ID = "medication.request.id";

    private static final String MEDICATION_REQUEST_INCLUDE_PARAMS =
            "MedicationRequest:encounter,MedicationRequest:medication,MedicationRequest:patient";

    @Autowired
    private MedicationRequestProcessor medicationRequestProcessor;

    @Override
    public void configure() {
        // spotless:off
        from("direct:fhir-medicationrequest")
                .routeId(MEDICATION_REQUEST_TO_SALE_ORDER_ROUTER)
                .process(exchange -> {
                    MedicationRequest medicationRequest = exchange.getMessage().getBody(MedicationRequest.class);
                    exchange.setProperty(Constants.FHIR_RESOURCE_TYPE, medicationRequest.fhirType());
                    exchange.setProperty(
                            MEDICATION_REQUEST_ID,
                            medicationRequest.getIdElement().getIdPart());
                    exchange.getMessage().setBody(medicationRequest);
                })
                .toD("openmrs-fhir://?id=${exchangeProperty." + MEDICATION_REQUEST_ID + "}&resource=${exchangeProperty."
                        + Constants.FHIR_RESOURCE_TYPE + "}&include=" + MEDICATION_REQUEST_INCLUDE_PARAMS)
                .to("direct:medication-request-to-sale-order-processor")
                .end();

        from("direct:medication-request-to-sale-order-processor")
                .routeId(MEDICATION_REQUEST_TO_SALE_ORDER_PROCESSOR)
                .process(medicationRequestProcessor)
                .log(
                        LoggingLevel.DEBUG,
                        "MedicationRequest with ID ${exchangeProperty." + MEDICATION_REQUEST_ID + "} processed.")
                .end();
        // spotless:on
    }
}
