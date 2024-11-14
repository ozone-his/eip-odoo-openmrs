package com.ozonehis.eip.odoo.openmrs.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OdooUtilsTest {

    @Test
    void shouldReturnDateInyyyy_MM_ddGivenDateInEEE_MMM_ddFormat() {
        // Setup
        String date="Tue Dec 30 04:28:58 IST 1997";

        // Act
        String result=OdooUtils.convertEEE_MMM_ddDateToOdooFormat(date);

        // Verify
        assertEquals("1997-12-30",result);
    }

    @Test
    void shouldReturnEmptyDateGivenDateInDifferentFormat() {
        // Setup
        String date="Tue 30 04:28:58 IST 1997";

        // Act
        String result=OdooUtils.convertEEE_MMM_ddDateToOdooFormat(date);

        // Verify
        assertEquals("",result);
    }
}