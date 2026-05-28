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
import static org.mockito.Mockito.when;

import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
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
}
