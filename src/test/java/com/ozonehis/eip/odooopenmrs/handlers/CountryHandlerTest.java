package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

class CountryHandlerTest {
    @Mock
    private OdooClient odooClient;

    @InjectMocks
    private CountryHandler countryHandler;

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
    public void shouldReturnCountryIdWhenOnlyOneCountryExistsWithId() throws MalformedURLException, XmlRpcException {
        // Setup
        String countryName = "India";
        Integer[] countryIds = {10};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName)))
                .thenReturn(countryIds);

        // Act
        Integer id = countryHandler.getCountryId(countryName);

        // Verify
        assertNotNull(id);
        assertEquals(10, id);
    }

    @Test
    public void shouldThrowErrorWhenMultipleCountriesExistsWithSameId() throws MalformedURLException, XmlRpcException {
        // Setup
        String countryName = "India";
        Integer[] countryIds = {10, 11};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName)))
                .thenReturn(countryIds);

        // Verify
        assertThrows(EIPException.class, () -> countryHandler.getCountryId(countryName));
    }

    @Test
    public void shouldReturnNullWhenNoCountryFoundWithId() throws MalformedURLException, XmlRpcException {
        // Setup
        String countryName = "India";
        Integer[] countryIds = {};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName)))
                .thenReturn(countryIds);

        // Act
        Integer id = countryHandler.getCountryId(countryName);

        // Verify
        assertNull(id);
    }
}
