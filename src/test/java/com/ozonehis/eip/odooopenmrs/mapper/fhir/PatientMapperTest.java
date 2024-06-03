package com.ozonehis.eip.odooopenmrs.mapper.fhir;

import com.ozonehis.eip.odooopenmrs.model.Partner;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientMapperTest {

    private PatientMapper patientMapper;

    @BeforeEach
    public void setup() {
        patientMapper = new PatientMapper();
    }

    @Test
    public void shouldMapOdooPartnerToFhirPatient() {
        // Setup
        Partner partner = new Partner();
        partner.setPartnerRef("123");

        //Act
        Patient patient = patientMapper.toFhir(partner);

        // Assert
        assertEquals("123", patient.getId());
    }

}