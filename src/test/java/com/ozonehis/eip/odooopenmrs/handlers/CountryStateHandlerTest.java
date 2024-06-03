package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import java.net.MalformedURLException;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class CountryStateHandlerTest {
    @Mock
    private OdooClient odooClient;

    @InjectMocks
    private CountryStateHandler countryStateHandler;

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
    public void shouldReturnCountryStateIdWhenOnlyOneIdFound() throws MalformedURLException, XmlRpcException {
        // Setup
        String countryStateName = "Berlin";
        Integer[] countryIds = {10};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_STATE_MODEL, asList("name", "=", countryStateName)))
                .thenReturn(countryIds);

        // Act
        Integer id = countryStateHandler.getStateId(countryStateName);

        // Verify
        assertNotNull(id);
        assertEquals(10, id);
    }

    @Test
    public void shouldThrowErrorWhenMultipleIdsAreFoundWithSameName() throws MalformedURLException, XmlRpcException {
        // Setup
        String countryStateName = "Berlin";
        Integer[] countryIds = {10, 11};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_STATE_MODEL, asList("name", "=", countryStateName)))
                .thenReturn(countryIds);

        // Verify
        assertThrows(EIPException.class, () -> countryStateHandler.getStateId(countryStateName));
    }

    @Test
    public void shouldReturnNullWhenNoCountryFoundWithName() throws MalformedURLException, XmlRpcException {
        // Setup
        String countryStateName = "Berlin";
        Integer[] countryIds = {};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_STATE_MODEL, asList("name", "=", countryStateName)))
                .thenReturn(countryIds);

        // Act
        Integer id = countryStateHandler.getStateId(countryStateName);

        // Verify
        assertNull(id);
    }
}
