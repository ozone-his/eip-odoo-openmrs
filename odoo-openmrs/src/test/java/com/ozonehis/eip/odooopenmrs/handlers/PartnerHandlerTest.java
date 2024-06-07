/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.EIPException;

class PartnerHandlerTest {

    private static final String PARTNER_REF_ID = "4ed050e1-c1be-4b4c-b407-c48d2db49b87";

    private static final String PATIENT_ID = "5td050e1-c1be-4b4c-b407-c48d2db49b65";

    private static final String PARTNER_IDENTIFIER_ID = "12d050e1-c1be-4b4c-b407-c48d2db49b78";

    @Mock
    private OdooClient odooClient;

    @Mock
    private PartnerMapper partnerMapper;

    @InjectMocks
    private PartnerHandler partnerHandler;

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
    public void shouldReturnPartnerWhenOnlyOnePartnerExists() throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> partner = getPartnerMap();

        Object[] partners = {partner};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", PARTNER_REF_ID)),
                        Constants.partnerDefaultAttributes))
                .thenReturn(partners);

        // Act
        Partner fetchedPartner = partnerHandler.partnerExists(PARTNER_REF_ID);

        // Verify
        assertNotNull(fetchedPartner);
        assertEquals("John Doe", fetchedPartner.getPartnerName());
        assertEquals(PARTNER_REF_ID, fetchedPartner.getPartnerRef());
        assertEquals(PARTNER_IDENTIFIER_ID, fetchedPartner.getPartnerComment());
        assertEquals(12, fetchedPartner.getPartnerId());
    }

    @Test
    public void shouldThrowErrorWhenMultiplePartnersWithSameIdExists() throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> partner1 = getPartnerMap();
        Map<String, Object> partner2 = getPartnerMap();

        Object[] partners = {partner1, partner2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", PARTNER_REF_ID)),
                        Constants.partnerDefaultAttributes))
                .thenReturn(partners);

        // Verify
        assertThrows(EIPException.class, () -> partnerHandler.partnerExists(PARTNER_REF_ID));
    }

    @Test
    public void shouldReturnPartnerIdAndUpdatePartnerWhenPartnerExists() throws MalformedURLException, XmlRpcException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PARTNER_REF_ID);

        Map<String, Object> headers = new HashMap<>();
        headers.put(
                Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(getPartner().getPartnerId()));

        Map<String, Object> partner = getPartnerMap();
        Object[] partners = {partner};

        // Mock behavior
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);
        when(odooClient.searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", patient.getIdPart())),
                        Constants.partnerDefaultAttributes))
                .thenReturn(partners);
        when(partnerMapper.toOdoo(patient)).thenReturn(getPartner());

        // Act
        int result = partnerHandler.ensurePartnerExistsAndUpdate(producerTemplate, patient);

        // Verify
        assertEquals(12, result);
        verify(producerTemplate, times(1))
                .sendBodyAndHeaders(eq("direct:odoo-update-partner-route"), eq(getPartner()), eq(headers));
    }

    @Test
    public void shouldReturnPartnerIdAndCreatePartnerWhenPartnerDoesNotExists()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Patient patient = new Patient();
        patient.setId(PARTNER_REF_ID);

        Map<String, Object> headers = new HashMap<>();

        // Mock behavior
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);
        when(odooClient.searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", patient.getIdPart())),
                        Constants.partnerDefaultAttributes))
                .thenReturn(new Object[] {})
                .thenReturn(new Object[] {getPartnerMap()});
        when(partnerMapper.toOdoo(patient)).thenReturn(getPartner());

        // Act
        int result = partnerHandler.ensurePartnerExistsAndUpdate(producerTemplate, patient);

        // Verify
        assertEquals(12, result);
        verify(producerTemplate, times(1))
                .sendBodyAndHeaders(eq("direct:odoo-create-partner-route"), eq(getPartner()), eq(headers));
    }

    private Map<String, Object> getPartnerMap() {
        Map<String, Object> partnerMap = new HashMap<>();
        partnerMap.put("id", 12);
        partnerMap.put("name", "John Doe");
        partnerMap.put("ref", PARTNER_REF_ID);
        partnerMap.put("city", "Berlin");
        partnerMap.put("active", true);
        partnerMap.put("comment", PARTNER_IDENTIFIER_ID);
        return partnerMap;
    }

    private Partner getPartner() {
        return OdooUtils.convertToObject(getPartnerMap(), Partner.class);
    }
}
