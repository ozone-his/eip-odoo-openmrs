package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import com.ozonehis.eip.odooopenmrs.handlers.CountryHandler;
import com.ozonehis.eip.odooopenmrs.handlers.CountryStateHandler;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class PartnerMapperTest {
    private static final String ADDRESS_EXTENSION_URL = "http://fhir.openmrs.org/ext/address";

    private static final String ADDRESS1_EXTENSION = "http://fhir.openmrs.org/ext/address#address1";

    private static final String ADDRESS2_EXTENSION = "http://fhir.openmrs.org/ext/address#address2";

    @Mock
    private CountryHandler countryHandler;

    @Mock
    private CountryStateHandler countryStateHandler;

    @InjectMocks
    private PartnerMapper partnerMapper;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    public void shouldMapFhirPatientToOdooPartner() {
        // Setup
        Patient patient = getPatient();

        // Mock behavior
        when(countryHandler.getCountryId("Test Country")).thenReturn(1);
        when(countryStateHandler.getStateId("Test State")).thenReturn(2);

        // Act
        Partner partner = partnerMapper.toOdoo(patient);

        // Assert
        assertEquals("123", partner.getPartnerRef());
        assertEquals("John Doe", partner.getPartnerName());
        assertEquals(true, partner.getPartnerActive());
        assertEquals("10IDH12H", partner.getPartnerComment());
        assertEquals("Test City", partner.getPartnerCity());
        assertEquals(1, partner.getPartnerCountryId());
        assertEquals("12345", partner.getPartnerZip());
        assertEquals(2, partner.getPartnerStateId());
        assertEquals("Test Address Line 1", partner.getPartnerStreet());
    }

    @Test
    public void whenCountryNotFoundThenShouldMapFhirPatientToOdooPartnerWithEmptyCountry() {
        // Setup
        Patient patient = getPatient();

        // Mock behavior
        when(countryHandler.getCountryId("Test Country")).thenReturn(null);
        when(countryStateHandler.getStateId("Test State")).thenReturn(2);

        // Act
        Partner partner = partnerMapper.toOdoo(patient);

        // Assert
        assertNull(partner.getPartnerCountryId());
        assertEquals("123", partner.getPartnerRef());
        assertEquals("Test City", partner.getPartnerCity());
        assertEquals("12345", partner.getPartnerZip());
        assertEquals(2, partner.getPartnerStateId());
        assertEquals("Test Address Line 1", partner.getPartnerStreet());
    }

    @Test
    public void whenStateNotFoundThenShouldMapFhirPatientToOdooPartnerWithEmptyCountry() {
        // Setup
        Patient patient = getPatient();

        // Mock behavior
        when(countryHandler.getCountryId("Test Country")).thenReturn(1);
        when(countryStateHandler.getStateId("Test State")).thenReturn(null);

        // Act
        Partner partner = partnerMapper.toOdoo(patient);

        // Assert
        assertNull(partner.getPartnerStateId());
        assertEquals("123", partner.getPartnerRef());
        assertEquals("Test City", partner.getPartnerCity());
        assertEquals("12345", partner.getPartnerZip());
        assertEquals(1, partner.getPartnerCountryId());
        assertEquals("Test Address Line 1", partner.getPartnerStreet());
    }

    private static Patient getPatient() {
        Patient patient = new Patient();
        patient.setId("123");
        patient.setActive(true);
        patient.setName(Collections.singletonList(
                new HumanName().setFamily("Doe").addGiven("John").setText("John Doe")));
        patient.setIdentifier(Collections.singletonList(
                new Identifier().setUse(Identifier.IdentifierUse.OFFICIAL).setValue("10IDH12H")));
        patient.setAddress(Collections.singletonList(getFhirAddress()));
        return patient;
    }

    private static Address getFhirAddress() {
        Address fhirAddress = new Address();
        fhirAddress.setCity("Test City");
        fhirAddress.setCountry("Test Country");
        fhirAddress.setPostalCode("12345");
        fhirAddress.setState("Test State");
        fhirAddress.setUse(Address.AddressUse.HOME);
        fhirAddress
                .addExtension()
                .setUrl(ADDRESS_EXTENSION_URL)
                .addExtension(new Extension(
                        ADDRESS1_EXTENSION, new StringType("Test Address Line 1")))
                .addExtension(new Extension(
                        ADDRESS2_EXTENSION, new StringType("Test Address Line 2")));
        return fhirAddress;
    }
}