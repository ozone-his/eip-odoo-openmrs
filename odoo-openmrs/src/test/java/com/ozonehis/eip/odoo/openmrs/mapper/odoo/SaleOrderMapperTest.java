/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper.odoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaleOrderMapperTest {

    private SaleOrderMapper saleOrderMapper;

    @BeforeEach
    public void setup() {
        saleOrderMapper = new SaleOrderMapper();
    }

    @Test
    public void shouldMapFhirEncounterToOdooSaleOrder() {
        // setup
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference("Encounter/1234"));

        // Act
        SaleOrder saleOrder = saleOrderMapper.toOdoo(encounter);

        // verify
        assertNotNull(saleOrder);
        assertEquals("1234", saleOrder.getOrderClientOrderRef());
        assertEquals("Sales Order", saleOrder.getOrderTypeName());
        assertEquals("draft", saleOrder.getOrderState());
    }

    @Test
    public void shouldReturnNullWhenEncounterIsNull() {
        // Act
        SaleOrder saleOrder = saleOrderMapper.toOdoo(null);

        // verify
        assertNull(saleOrder);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenEncounterPartOfReferenceIsNull() {
        // setup
        Encounter encounter = new Encounter();

        // Act and verify
        assertThrows(IllegalArgumentException.class, () -> saleOrderMapper.toOdoo(encounter));
    }
}
