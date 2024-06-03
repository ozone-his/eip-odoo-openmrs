package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Timing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleOrderLineMapperTest {

    public static final String MEDICATION_REQUEST_ID = "1cdda1ce-7f98-4bc7-9d20-8b4e953d972a";

    public static final String MEDICATION_ID = "c95ab33a-8558-4705-8a7a-3b4e580270c7";

    private static final String SERVICE_REQUEST_ID = "4ed050e1-c1be-4b4c-b407-c48d2db49b87";

    private static final String TABLET_UNIT = "Tablet";

    private static final String COMPLETE_BLOOD_COUNT_CODE = "446ac22c-24f2-40ad-98f4-65f026f434e9";

    private static final String COMPLETE_BLOOD_COUNT_DISPLAY = "Complete Blood Count";

    private static final String COMPLETE_BLOOD_COUNT_ID = "3d597a15-b08f-4c35-8a42-0c15f9a19fa6";

    private static final String PRACTITIONER_ID = "e5ca6578-fb37-4900-a054-c68db82a551c";

    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    @BeforeEach
    void setUp() {
        saleOrderLineMapper = new SaleOrderLineMapper<>();
    }

    @Test
    void shouldMapServiceRequestToSaleOrderLine() {
        // setup
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(SERVICE_REQUEST_ID);

        // code
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode(COMPLETE_BLOOD_COUNT_CODE);
        coding.setDisplay(COMPLETE_BLOOD_COUNT_DISPLAY);
        coding.setId(COMPLETE_BLOOD_COUNT_ID);
        codeableConcept.addCoding(coding);
        codeableConcept.setText(COMPLETE_BLOOD_COUNT_DISPLAY);
        serviceRequest.setCode(codeableConcept);

        Reference requester = new Reference();
        requester.setId("1cdda1ce-7f98-4bc7-9d20-8b4e953d972a");
        requester.setReference("Practitioner/" + PRACTITIONER_ID);
        requester.setDisplay("John Doe");
        serviceRequest.setRequester(requester);

        // Act
        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(serviceRequest);

        // verify
        assertNotNull(saleOrderLine);
        assertEquals(1.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(COMPLETE_BLOOD_COUNT_DISPLAY + " | Orderer: John Doe", saleOrderLine.getSaleOrderLineName());
    }

    @Test
    void shouldMapMedicationRequestToSaleOrderLine() {
        // setup
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setId(MEDICATION_REQUEST_ID);

        // dispense request
        medicationRequest.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setQuantity(new Quantity().setValue(10)));
        medicationRequest.setMedication(new Reference("Medication/" + MEDICATION_ID).setDisplay("medication"));
        medicationRequest.setRequester(new Reference().setDisplay("requester"));
        MedicationRequest.MedicationRequestDispenseRequestComponent dispenseRequest =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();

        // quantity
        Quantity quantity = new Quantity();
        quantity.setValue(10);
        quantity.setUnit(TABLET_UNIT);
        quantity.setCode("15AAAAAAAAAAA");
        dispenseRequest.setQuantity(quantity);
        medicationRequest.setDispenseRequest(dispenseRequest);

        // dosage instruction
        Dosage.DosageDoseAndRateComponent doseAndRate = new Dosage.DosageDoseAndRateComponent();
        Quantity doseQuantity = new Quantity();
        doseQuantity.setValue(7);
        doseQuantity.setUnit(TABLET_UNIT);
        doseAndRate.setDose(doseQuantity);
        Dosage dosage = new Dosage();
        dosage.setDoseAndRate(List.of(doseAndRate));

        Timing timing = new Timing();
        timing.setCode(new CodeableConcept().setText("thrice daily"));
        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
        repeat.setFrequency(3);
        repeat.setDuration(10);
        repeat.setDurationUnit(Timing.UnitsOfTime.D);
        timing.setRepeat(repeat);
        dosage.setTiming(timing);
        medicationRequest.setDosageInstruction(List.of(dosage));

        // Act
        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(medicationRequest);

        // verify
        assertNotNull(saleOrderLine);
        assertEquals(10.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals("15AAAAAAAAAAA", saleOrderLine.getSaleOrderLineProductUom());
        assertEquals(
                "medication | 10.0 Tablet | 7 Tablet - thrice daily - 10 day | Orderer: requester",
                saleOrderLine.getSaleOrderLineName());
    }

    @Test
    @DisplayName("Should throw exception for unsupported resource type")
    void shouldThrowExceptionForUnsupportedResourceType() {
        Resource unsupportedResource = new Patient();
        assertThrows(IllegalArgumentException.class, () -> saleOrderLineMapper.toOdoo(unsupportedResource));
    }
}
