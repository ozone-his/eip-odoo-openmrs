package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SaleOrderMapperTest {

    private SaleOrderMapper saleOrderMapper;

    @BeforeEach
    void setup() {
        saleOrderMapper = new SaleOrderMapper();
    }

    @Test
    public void shouldMapFhirEncounterToOdooSaleOrder() {
        // setup
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference("Encounter/1234"));

        // Act
        SaleOrder saleOrder = saleOrderMapper.toOdoo(encounter);

        //verify
        assertNotNull(saleOrder);
        assertEquals("1234", saleOrder.getOrderClientOrderRef());
        assertEquals("Sale Order", saleOrder.getOrderTypeName());
        assertEquals("draft", saleOrder.getOrderState());
    }

    @Test
    public void shouldReturnNullWhenEncounterIsNull() {
        // Act
        SaleOrder saleOrder = saleOrderMapper.toOdoo(null);

        //verify
        assertNull(saleOrder);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenEncounterPartOfReferenceIsNull() {
        // setup
        Encounter encounter = new Encounter();

        // Act and verify
        assertThrows(IllegalArgumentException.class, () -> saleOrderMapper.toOdoo(encounter));
    }
}