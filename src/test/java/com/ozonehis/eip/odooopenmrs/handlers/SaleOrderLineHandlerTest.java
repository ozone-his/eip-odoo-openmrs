package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class SaleOrderLineHandlerTest {

    @Mock
    private OdooClient odooClient;

    @Mock
    private ProductHandler productHandler;

    @Mock
    private UomHandler uomHandler;

    @Mock
    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    @InjectMocks
    private SaleOrderLineHandler saleOrderLineHandler;

    private static AutoCloseable mocksCloser;

    private static final int ORDER_ID = 14;

    private static final int PRODUCT_ID = 15;

    private static final int PRODUCT_UOM_ID = 16;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    public void shouldReturnSaleOrderLineWhenOnlyOneSaleOrderLineExistsWithOrderIdAndProductId()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> saleOrderLineMap =
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID);
        Object[] saleOrderLines = {saleOrderLineMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);

        // Act
        SaleOrderLine result = saleOrderLineHandler.getSaleOrderLineIfExists(ORDER_ID, PRODUCT_ID);

        // Verify
        assertNotNull(result);
        assertEquals(ORDER_ID, result.getSaleOrderLineOrderId());
        assertEquals("Aspirin 81 mg | 10 Tablet", result.getSaleOrderLineName());
        assertEquals(5.0f, result.getSaleOrderLineProductUomQty());
    }

    @Test
    public void shouldThrowErrorWhenMultipleSaleOrderLineExistsWithSameOrderIdAndProductId()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> saleOrderLineMap1 =
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID);
        Map<String, Object> saleOrderLineMap2 =
                getSaleOrderLineMap(2, "Aspirin 361 mg | 5 Tablet", ORDER_ID, PRODUCT_ID, 7.0f, PRODUCT_UOM_ID);

        Object[] saleOrderLines = {saleOrderLineMap1, saleOrderLineMap2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);

        // Verify
        assertThrows(EIPException.class, () -> saleOrderLineHandler.getSaleOrderLineIfExists(ORDER_ID, PRODUCT_ID));
    }

    @Test
    public void shouldReturnNullWhenNoSaleOrderLineExistsWithOrderIdAndProductId()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Object[] saleOrderLines = {};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);

        // Act
        SaleOrderLine result = saleOrderLineHandler.getSaleOrderLineIfExists(ORDER_ID, PRODUCT_ID);

        // Verify
        assertNull(result);
    }

    public Map<String, Object> getSaleOrderLineMap(
            int id, String name, int orderId, int productId, float productUomQty, int productUom) {
        Map<String, Object> saleOrderMap = new HashMap<>();
        saleOrderMap.put("id", id);
        saleOrderMap.put("name", name);
        saleOrderMap.put("order_id", orderId);
        saleOrderMap.put("product_id", productId);
        saleOrderMap.put("product_uom_qty", productUomQty);
        saleOrderMap.put("product_uom", productUom);
        return saleOrderMap;
    }
}
