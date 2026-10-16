/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

class OdooUtilsTest {

    private OdooUtils odooUtils;

    @BeforeEach
    void setup() {
        Environment mockEnvironment = Mockito.mock(Environment.class);
        when(mockEnvironment.getProperty("odoo.customer.weight.field")).thenReturn("x_customer_weight");
        when(mockEnvironment.getProperty("odoo.customer.dob.field")).thenReturn("x_customer_dob");
        when(mockEnvironment.getProperty("odoo.customer.id.field")).thenReturn("x_external_identifier");
        odooUtils = new OdooUtils();
        odooUtils.setEnvironment(mockEnvironment);
    }

    @Test
    void shouldReturnDateInyyyy_MM_ddGivenDateInEEE_MMM_ddFormat() {
        // Setup
        String date = "Tue Dec 30 04:28:58 IST 1997";

        // Act
        String result = OdooUtils.convertEEEMMMddDateToOdooFormat(date);

        // Verify
        assertEquals("1997-12-30", result);
    }

    @Test
    void shouldReturnEmptyDateGivenDateInDifferentFormat() {
        // Setup
        String date = "Tue 30 04:28:58 IST 1997";

        // Act
        String result = OdooUtils.convertEEEMMMddDateToOdooFormat(date);

        // Verify
        assertEquals("", result);
    }

    @Test
    void shouldExcludeNullFieldsWhenConvertingObjectToMap() throws Exception {
        // Setup
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderClientOrderRef("visit-123");
        saleOrder.setOrderState("draft");

        // Act
        Map<String, Object> result = odooUtils.convertObjectToMap(saleOrder);

        // Verify
        assertEquals("visit-123", result.get("client_order_ref"));
        assertEquals("draft", result.get("state"));
        assertFalse(result.containsKey("company_id"));
        assertFalse(result.containsKey("partner_id"));
    }

    @Test
    void shouldNormalizeMany2OneListToIntegerIdWhenConvertingToObject() {
        // Setup
        Map<String, Object> data = Map.of("base_insurance_id", List.of(12, "Insurance 50%"));

        // Act
        Many2OneInteger result = odooUtils.convertToObject(data, Many2OneInteger.class);

        // Verify
        assertEquals(12, result.getBaseInsuranceId());
    }

    @Test
    void shouldNormalizeMany2OneObjectArrayToIntegerIdWhenConvertingToObject() {
        // Setup
        Map<String, Object> data = Map.of("base_insurance_id", new Object[] {7, "Insurance 100%"});

        // Act
        Many2OneInteger result = odooUtils.convertToObject(data, Many2OneInteger.class);

        // Verify
        assertEquals(7, result.getBaseInsuranceId());
    }

    @Test
    void shouldPassThroughPlainIntegerWhenConvertingToObject() {
        // Setup
        Map<String, Object> data = Map.of("base_insurance_id", 42);

        // Act
        Many2OneInteger result = odooUtils.convertToObject(data, Many2OneInteger.class);

        // Verify
        assertEquals(42, result.getBaseInsuranceId());
    }

    @Test
    void shouldMapEmptyMany2OneListToNullWhenConvertingToObject() {
        // Setup
        Map<String, Object> data = Map.of("base_insurance_id", List.of());

        // Act
        Many2OneInteger result = odooUtils.convertToObject(data, Many2OneInteger.class);

        // Verify
        assertNull(result.getBaseInsuranceId());
    }

    /** Minimal stand-in for a model class carrying an Odoo many2one id. */
    static class Many2OneInteger {

        @JsonProperty("base_insurance_id")
        private Integer baseInsuranceId;

        public Integer getBaseInsuranceId() {
            return baseInsuranceId;
        }

        public void setBaseInsuranceId(Integer baseInsuranceId) {
            this.baseInsuranceId = baseInsuranceId;
        }
    }
}
