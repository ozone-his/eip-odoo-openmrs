/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
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
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class EncounterProcessorTest extends BaseProcessorTest {

    @Mock
    private ProductHandler products;

    @Mock
    private PatientHandler patients;

    @Mock
    private PartnerHandler partners;

    @Mock
    private SaleOrderHandler orders;

    private final Partner partner = new Partner();

    private final Patient patient = new Patient();

    @InjectMocks
    private EncounterProcessor encounterProcessor;

    private AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
        encounterProcessor.setVisitSyncEnabled(true);
        encounterProcessor.setEncounterHandler(new EncounterHandler());
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
        mocksCloser.close();
    }

    @Test
    void createsPatientSaleOrderAndLineWhenVisitStarts() throws Exception {
        Encounter encounter = openVisit();
        Exchange exchange = createExchange(encounter, "c");
        encounterProcessor.process(exchange);
        verify(partners).createOrUpdatePartner(any(), eq(patient), isNull());
        verify(orders)
                .createSaleOrderWithSaleOrderLine(
                        eq(encounter), eq(encounter), eq(partner), eq("visit-123"), eq("patient-123"), isNull(), any());
        verify(orders, never())
                .updateSaleOrderIfExistsWithSaleOrderLine(any(), any(), any(), anyInt(), any(), any(), any());
    }

    @Test
    void addsVisitLineToExistingSaleOrder() throws Exception {
        Encounter encounter = openVisit();
        SaleOrder order = new SaleOrder();
        Exchange exchange = createExchange(encounter, "c");

        when(orders.getDraftSaleOrderIfExistsByVisitId("visit-123", null)).thenReturn(order);
        encounterProcessor.process(exchange);
        verify(orders)
                .updateSaleOrderIfExistsWithSaleOrderLine(
                        eq(encounter), eq(order), eq("visit-123"), eq(12), eq("patient-123"), isNull(), any());
        verify(orders, never()).createSaleOrderWithSaleOrderLine(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void ignoresClinicalEncounters() throws Exception {
        Exchange exchange = createExchange(new Encounter(), "c");

        encounterProcessor.process(exchange);

        verifyNoInteractions(products, patients, partners, orders);
    }

    @Test
    void ignoresVisitsWhenSyncIsDisabled() throws Exception {
        Exchange exchange = createExchange(openVisit(), "c");
        encounterProcessor.setVisitSyncEnabled(false);

        encounterProcessor.process(exchange);

        verifyNoInteractions(products, patients, partners, orders);
    }

    @Test
    void missingProductFailsBeforeCreatingQuotation() {
        Exchange exchange = createExchange(openVisit(), "c");

        when(products.getProduct(any())).thenReturn(null);
        EIPException exception = assertThrows(EIPException.class, () -> encounterProcessor.process(exchange));
        assertEquals("No Odoo product mapped to visit type type-123", exception.getMessage());
        verifyNoInteractions(patients, partners, orders);
    }

    @Test
    void missingPatientFailsBeforeCreatingQuotation() {
        Encounter encounter = openVisit();
        Exchange exchange = createExchange(encounter, "c");

        encounter.setSubject(null);
        EIPException exception = assertThrows(EIPException.class, () -> encounterProcessor.process(exchange));
        assertEquals("Visit has no patient reference: visit-123", exception.getMessage());
        verifyNoInteractions(patients, partners, orders);
    }

    private Encounter openVisit() {
        Encounter visit = visit("2026-10-20T08:00:00Z", null);
        visit.setSubject(new Reference("Patient/patient-123"));
        return visit;
    }

    private Encounter visit(String start, String end) {
        Encounter visit = new Encounter();
        visit.setId("visit-123");
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
