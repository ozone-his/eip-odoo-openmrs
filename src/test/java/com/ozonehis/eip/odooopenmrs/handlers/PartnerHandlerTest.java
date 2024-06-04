package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class PartnerHandlerTest {

    private static final String PARTNER_REF_ID = "4ed050e1-c1be-4b4c-b407-c48d2db49b87";

    private static final String PARTNER_IDENTIFIER_ID = "12d050e1-c1be-4b4c-b407-c48d2db49b78";

    @Mock
    private OdooClient odooClient;

    @InjectMocks
    private PartnerHandler partnerHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    public void shouldReturnPartnerWhenOnlyOnePartnerExists() throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> partner = getPartner();

        Object[] partners = {partner};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", PARTNER_REF_ID)),
                        Constants.partnerDefaultAttributes))
                .thenReturn(partners);

        // Act
        Partner fetchedPartner = partnerHandler.partnerExists(PARTNER_REF_ID);

        // Verify
        assertNotNull(fetchedPartner);
        assertEquals("John Doe", fetchedPartner.getPartnerName());
        assertEquals(PARTNER_REF_ID, fetchedPartner.getPartnerRef());
        assertEquals(PARTNER_IDENTIFIER_ID, fetchedPartner.getPartnerComment());
        assertEquals(12, fetchedPartner.getPartnerId());
    }

    @Test
    public void shouldThrowErrorWhenMultiplePartnersWithSameIdExists() throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> partner1 = getPartner();
        Map<String, Object> partner2 = getPartner();

        Object[] partners = {partner1, partner2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", PARTNER_REF_ID)),
                        Constants.partnerDefaultAttributes))
                .thenReturn(partners);

        // Verify
        assertThrows(EIPException.class, () -> partnerHandler.partnerExists(PARTNER_REF_ID));
    }

    public Map<String, Object> getPartner() {
        Map<String, Object> partnerMap = new HashMap<>();
        partnerMap.put("id", 12);
        partnerMap.put("name", "John Doe");
        partnerMap.put("ref", PARTNER_REF_ID);
        partnerMap.put("city", "Berlin");
        partnerMap.put("active", true);
        partnerMap.put("comment", PARTNER_IDENTIFIER_ID);
        return partnerMap;
    }
}
