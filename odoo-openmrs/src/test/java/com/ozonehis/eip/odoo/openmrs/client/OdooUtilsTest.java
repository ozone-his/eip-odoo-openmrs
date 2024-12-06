/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.client;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

class OdooUtilsTest {

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
}
