/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CompanyHandler;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class CompanyHandlerTest {

    private static final String EXTERNAL_ID = "8d6c993e-c2cc-11de-8d13-0010c6dffd0f";

    private static final String ENCOUNTER_UUID = "1cdda1ce-7f98-4bc7-9d20-8b4e953d972a";

    @Mock
    private OdooClient odooClient;

    @InjectMocks
    private CompanyHandler companyHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    public void shouldReturnCompanyIdWhenExternalIdHasOneMappingWithIntegerResId() {
        // Setup
        Object[] companies = {getCompanyExternalIdMap(12)};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(companies);

        // Act
        Integer companyId = companyHandler.getCompanyIdByExternalId(EXTERNAL_ID);

        // Verify
        assertEquals(12, companyId);
    }

    @Test
    public void shouldReturnCompanyIdWhenExternalIdHasOneMappingWithNumericResId() {
        // Setup
        Object[] companies = {getCompanyExternalIdMap(12.0)};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(companies);

        // Act
        Integer companyId = companyHandler.getCompanyIdByExternalId(EXTERNAL_ID);

        // Verify
        assertEquals(12, companyId);
    }

    @Test
    public void shouldReturnNullAndSkipOdooLookupWhenExternalIdIsBlank() {
        // Act
        Integer companyId = companyHandler.getCompanyIdByExternalId("");

        // Verify
        assertNull(companyId);
        verify(odooClient, times(0)).searchAndRead(any(), any(), any());
    }

    @Test
    public void shouldReturnNullWhenNoCompanyFoundWithExternalId() {
        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(new Object[] {});

        // Act
        Integer companyId = companyHandler.getCompanyIdByExternalId(EXTERNAL_ID);

        // Verify
        assertNull(companyId);
    }

    @Test
    public void shouldThrowErrorWhenOdooReturnsNullForExternalIdLookup() {
        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(null);

        // Verify
        assertThrows(EIPException.class, () -> companyHandler.getCompanyIdByExternalId(EXTERNAL_ID));
    }

    @Test
    public void shouldThrowErrorWhenMultipleCompaniesFoundWithExternalId() {
        // Setup
        Object[] companies = {getCompanyExternalIdMap(12), getCompanyExternalIdMap(13)};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(companies);

        // Verify
        assertThrows(EIPException.class, () -> companyHandler.getCompanyIdByExternalId(EXTERNAL_ID));
    }

    @Test
    public void shouldThrowErrorWhenCompanyMappingHasUnexpectedResIdType() {
        // Setup
        Object[] companies = {getCompanyExternalIdMap("12")};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(companies);

        // Verify
        assertThrows(EIPException.class, () -> companyHandler.getCompanyIdByExternalId(EXTERNAL_ID));
    }

    @Test
    public void shouldReturnCompanyIdByEncounterLocation() {
        // Setup
        Encounter encounter = getEncounterWithLocation();
        Object[] companies = {getCompanyExternalIdMap(12)};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(companies);

        // Act
        Integer companyId = companyHandler.getCompanyIdByEncounterLocation(encounter);

        // Verify
        assertEquals(12, companyId);
    }

    @Test
    public void shouldReturnNullAndSkipOdooLookupWhenEncounterHasNoLocation() {
        // Setup
        Encounter encounter = new Encounter();
        encounter.setId(ENCOUNTER_UUID);

        // Act
        Integer companyId = companyHandler.getCompanyIdByEncounterLocation(encounter);

        // Verify
        assertNull(companyId);
        verify(odooClient, times(0)).searchAndRead(any(), any(), any());
    }

    @Test
    public void shouldReturnNullWhenNoCompanyFoundForEncounterLocation() {
        // Setup
        Encounter encounter = getEncounterWithLocation();

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.COMPANY_MODEL), asList("name", "=", EXTERNAL_ID)),
                        asList("res_id")))
                .thenReturn(new Object[] {});

        // Act
        Integer companyId = companyHandler.getCompanyIdByEncounterLocation(encounter);

        // Verify
        assertNull(companyId);
    }

    private Map<String, Object> getCompanyExternalIdMap(Object resId) {
        Map<String, Object> companyMap = new HashMap<>();
        companyMap.put("res_id", resId);
        return companyMap;
    }

    private Encounter getEncounterWithLocation() {
        Encounter encounter = new Encounter();
        encounter.setId(ENCOUNTER_UUID);
        encounter.addLocation().setLocation(new Reference("Location/" + EXTERNAL_ID));
        return encounter;
    }
}
