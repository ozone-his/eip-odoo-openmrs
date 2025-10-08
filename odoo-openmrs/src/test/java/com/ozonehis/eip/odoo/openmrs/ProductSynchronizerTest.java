/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import static com.ozonehis.eip.odoo.openmrs.ProductSynchronizer.RESOURCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IUpdate;
import ca.uhn.fhir.rest.gclient.IUpdateTyped;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.eip.odoo.openmrs.client.OdooFhirClient;
import com.ozonehis.eip.odoo.openmrs.client.OpenmrsRestClient;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.Medication.MedicationStatus;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProductSynchronizerTest {

    private static final String SOURCE_URI = "http://localhost";

    private static ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private OdooFhirClient mockOdooClient;

    @Mock
    private IGenericClient openmrsFhirClient;

    @Mock
    private OpenmrsRestClient mockOpenmrsClient;

    @Mock
    private IUpdate mockIUpdate;

    @Mock
    private IUpdateTyped mockIUpdateTyped;

    @Mock
    private MethodOutcome mockMethodOutcome;

    private ProductSynchronizer synchronizer;

    @BeforeEach
    public void setUp() {
        lenient().when(openmrsFhirClient.update()).thenReturn(mockIUpdate);
        synchronizer = new ProductSynchronizer(mockOdooClient, openmrsFhirClient, mockOpenmrsClient);
    }

    private Bundle createBundle(List<Medication> medications) {
        Bundle bundle = new Bundle();
        medications.forEach(m -> bundle.addEntry().setResource(m));
        return bundle;
    }

    private static void addExtension(Medication medication, String uri, String value) {
        Extension medExt = medication.getExtensionByUrl(Constants.FHIR_OPENMRS_FHIR_EXT_MEDICINE);
        if (medExt == null) {
            medExt = medication.addExtension().setUrl(Constants.FHIR_OPENMRS_FHIR_EXT_MEDICINE);
        }

        Extension ext = new Extension();
        ext.setUrl(uri);
        ext.setValue(new StringType(value));
        medExt.addExtension(ext);
    }

    @Test
    public void syncProducts_shouldCreateAProductInOpenMRSIfItDoesNotExist() throws Exception {
        final String name1 = "Tylenol";
        final String id1 = "some-uuid-1";
        final String code1 = "12345";
        Medication m1 = new Medication();
        m1.setId(id1);
        m1.setStatus(MedicationStatus.ACTIVE);
        m1.getCode().addCoding().setSystem(SOURCE_URI).setCode(code1);
        addExtension(m1, Constants.FHIR_OPENMRS_EXT_DRUG_NAME, name1);

        final String name2 = "Advil";
        final String id2 = "some-uuid-2";
        final String code2 = "67890";
        Medication m2 = new Medication();
        m2.setId(id2);
        m2.setStatus(MedicationStatus.ACTIVE);
        m2.getCode().addCoding().setSystem(SOURCE_URI).setCode(code2);
        addExtension(m2, Constants.FHIR_OPENMRS_EXT_DRUG_NAME, name2);
        final IUpdateTyped mockIUpdateTyped2 = Mockito.mock(IUpdateTyped.class);
        when(mockIUpdate.resource(m1)).thenReturn(mockIUpdateTyped);
        when(mockIUpdateTyped.execute()).thenReturn(mockMethodOutcome);
        when(mockIUpdate.resource(m2)).thenReturn(mockIUpdateTyped2);
        when(mockIUpdateTyped2.execute()).thenReturn(mockMethodOutcome);
        when(mockOdooClient.getAll(Medication.class)).thenReturn(createBundle(List.of(m1, m2)));

        synchronizer.syncProducts();

        Mockito.verify(mockIUpdateTyped).execute();
        Mockito.verify(mockIUpdateTyped2).execute();
        Mockito.verify(mockOpenmrsClient, never()).delete(any(), any());
    }

    @Test
    public void syncProducts_shouldRetireAnExistingDrugInOpenMRSIfArchivedInOdoo() throws Exception {
        final String name = "Tylenol";
        final String id = "some-uuid";
        Medication m = new Medication();
        m.setId(id);
        m.setStatus(MedicationStatus.INACTIVE);
        addExtension(m, Constants.FHIR_OPENMRS_EXT_DRUG_NAME, name);
        when(mockOdooClient.getAll(Medication.class)).thenReturn(createBundle(List.of(m)));
        Map<String, Object> drugData = Map.of("name", name, "retired", false);
        when(mockOpenmrsClient.get(RESOURCE, id)).thenReturn(MAPPER.writeValueAsBytes(drugData));
        when(mockIUpdate.resource(m)).thenReturn(mockIUpdateTyped);
        when(mockIUpdateTyped.execute()).thenReturn(mockMethodOutcome);

        synchronizer.syncProducts();

        Mockito.verify(mockIUpdateTyped).execute();
        Mockito.verify(mockOpenmrsClient).delete(eq(RESOURCE), eq(id));
    }

    @Test
    public void syncProducts_shouldRestoreARetiredExistingDrugInOpenMRSIfNotArchivedInOdoo() throws Exception {
        final String name = "Tylenol";
        final String id = "some-uuid";
        Medication m = new Medication();
        m.setId(id);
        m.setStatus(MedicationStatus.ACTIVE);
        addExtension(m, Constants.FHIR_OPENMRS_EXT_DRUG_NAME, name);
        when(mockOdooClient.getAll(Medication.class)).thenReturn(createBundle(List.of(m)));
        Map<String, Object> drugData = Map.of("name", name, "retired", true);
        when(mockOpenmrsClient.get(RESOURCE, id)).thenReturn(MAPPER.writeValueAsBytes(drugData));
        when(mockIUpdate.resource(m)).thenReturn(mockIUpdateTyped);
        when(mockIUpdateTyped.execute()).thenReturn(mockMethodOutcome);

        synchronizer.syncProducts();

        Mockito.verify(mockIUpdateTyped).execute();
        ArgumentCaptor<String> argCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockOpenmrsClient).createOrUpdate(eq(RESOURCE), eq(id), argCaptor.capture());
        String json = argCaptor.getAllValues().get(0);
        Map<String, Object> deserializedJson = MAPPER.readValue(json, Map.class);
        assertEquals(1, deserializedJson.size());
        assertEquals("false", deserializedJson.get("deleted"));
    }

    @Test
    public void syncProducts_shouldAddAnArchivedDrugToOpenMRSIfItDoesNotExist() throws Exception {
        final String name = "Tylenol";
        final String id = "some-uuid";
        Medication m = new Medication();
        m.setId(id);
        m.setStatus(MedicationStatus.INACTIVE);
        addExtension(m, Constants.FHIR_OPENMRS_EXT_DRUG_NAME, name);
        when(mockOdooClient.getAll(Medication.class)).thenReturn(createBundle(List.of(m)));

        synchronizer.syncProducts();

        Mockito.verify(mockOpenmrsClient, never()).createOrUpdate(any(), any(), any());
        Mockito.verify(mockOpenmrsClient, never()).delete(eq(RESOURCE), eq(id));
    }
}
