/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CompanyHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.ProductHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.PatientHandler;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import com.ozonehis.eip.odoo.openmrs.model.Product;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.time.Instant;
import java.util.Date;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openmrs.eip.EIPException;

class EncounterProcessorTest {
    private final EncounterProcessor processor = new EncounterProcessor();
    private final ProductHandler products = mock(ProductHandler.class);
    private final PatientHandler patients = mock(PatientHandler.class);
    private final PartnerHandler partners = mock(PartnerHandler.class);
    private final SaleOrderHandler orders = mock(SaleOrderHandler.class);
    private final CompanyHandler companies = mock(CompanyHandler.class);
    private final DefaultCamelContext context = new DefaultCamelContext();
    private final Partner partner = new Partner();
    private final Patient patient = new Patient();

    @BeforeEach
    void setup() throws Exception {
        context.start();
        processor.setVisitSyncEnabled(true);
        processor.setEncounterHandler(new EncounterHandler());
        processor.setProductHandler(products);
        processor.setPatientHandler(patients);
        processor.setPartnerHandler(partners);
        processor.setSaleOrderHandler(orders);
        Product product = new Product();
        product.setProductResId(42);
        when(products.getProduct(any())).thenReturn(product);
        patient.setId("patient-123");
        when(patients.getPatientByPatientID("patient-123")).thenReturn(patient);
        partner.setPartnerId(12);
        when(partners.createOrUpdatePartner(any(), eq(patient), any())).thenReturn(partner);
    }

    @AfterEach
    void close() throws Exception {
        context.close();
    }

    private Exchange exchange(Object body, String event) {
        Exchange exchange = new DefaultExchange(context);
        exchange.getMessage().setBody(body);
        exchange.getMessage().setHeader(HEADER_FHIR_EVENT_TYPE, event);
        return exchange;
    }

    private Encounter openVisit() {
        Encounter visit = visit("2026-10-20T08:00:00Z", null);
        visit.setSubject(new Reference("Patient/patient-123"));
        return visit;
    }

    @Test
    void createsPatientSaleOrderAndLineWhenVisitStarts() throws Exception {
        Encounter visit = openVisit();
        processor.process(exchange(visit, "c"));
        verify(partners).createOrUpdatePartner(any(), eq(patient), isNull());
        verify(orders)
                .createSaleOrderWithSaleOrderLine(
                        eq(visit), eq(visit), eq(partner), eq("visit-123"), eq("patient-123"), isNull(), any());
        verify(orders, never())
                .updateSaleOrderIfExistsWithSaleOrderLine(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void addsVisitLineToExistingSaleOrder() throws Exception {
        Encounter visit = openVisit();
        SaleOrder order = new SaleOrder();
        when(orders.getDraftSaleOrderIfExistsByVisitId("visit-123", null)).thenReturn(order);
        processor.process(exchange(visit, "c"));
        verify(orders)
                .updateSaleOrderIfExistsWithSaleOrderLine(
                        eq(visit), eq(order), eq("visit-123"), eq(12), eq("patient-123"), isNull(), any());
        verify(orders, never()).createSaleOrderWithSaleOrderLine(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ignoresClinicalEncountersAndDisabledSync() throws Exception {
        processor.process(exchange(new Encounter(), "c"));
        processor.setVisitSyncEnabled(false);
        processor.process(exchange(openVisit(), "c"));
        verifyNoInteractions(products, patients, partners, orders, companies);
    }

    @Test
    void missingProductFailsBeforeCreatingQuotation() {
        when(products.getProduct(any())).thenReturn(null);
        assertThrows(EIPException.class, () -> processor.process(exchange(openVisit(), "c")));
        verifyNoInteractions(patients, partners, orders);
    }

    @Test
    void missingPatientFailsBeforeCreatingQuotation() {
        Encounter visit = openVisit();
        visit.setSubject(null);
        assertThrows(EIPException.class, () -> processor.process(exchange(visit, "c")));
        verifyNoInteractions(patients, partners, orders);
    }

    public Encounter visit(String start, String end) {
        Encounter visit = new Encounter();
        visit.setId("visit-123");
        visit.getMeta().addTag(Constants.ENCOUNTER_TAG_SYSTEM, "visit", "Visit");
        visit.addType()
                .addCoding()
                .setSystem(Constants.VISIT_TYPE_SYSTEM)
                .setCode("type-123")
                .setDisplay("Inpatient");
        visit.getPeriod().setStart(Date.from(Instant.parse(start)));
        if (end != null) {
            visit.getPeriod().setEnd(Date.from(Instant.parse(end)));
        }
        return visit;
    }
}
