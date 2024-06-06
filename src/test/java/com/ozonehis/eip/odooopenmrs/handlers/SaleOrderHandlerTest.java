package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
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

class SaleOrderHandlerTest {

    @Mock
    private OdooClient odooClient;

    @Mock
    private SaleOrderLineHandler saleOrderLineHandler;

    @Mock
    private SaleOrderMapper saleOrderMapper;

    @Mock
    private ProductHandler productHandler;

    @InjectMocks
    private SaleOrderHandler saleOrderHandler;

    private static AutoCloseable mocksCloser;

    private static final String VISIT_ID_1 = "e5ca6578-fb37-4900-a054-c68db82a551c";

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    public void shouldReturnSaleOrderWhenOnlyOneSaleOrderExistsWithVisitId()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> saleOrderMap = getSaleOrderMap(1, VISIT_ID_1, "draft", 12);
        Object[] saleOrders = {saleOrderMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(saleOrders);

        // Act
        SaleOrder result = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(VISIT_ID_1);

        // Verify
        assertNotNull(result);
        assertEquals(VISIT_ID_1, result.getOrderClientOrderRef());
        assertEquals(1, result.getOrderId());
        assertEquals("draft", result.getOrderState());
        assertEquals(12, result.getOrderPartnerId());
    }

    @Test
    public void shouldThrowErrorWhenMultipleSaleOrderExistsWithSameVisitId()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Map<String, Object> saleOrderMap1 = getSaleOrderMap(1, VISIT_ID_1, "draft", 12);
        Map<String, Object> saleOrderMap2 = getSaleOrderMap(2, VISIT_ID_1, "draft", 15);

        Object[] saleOrders = {saleOrderMap1, saleOrderMap2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(saleOrders);

        // Verify
        assertThrows(EIPException.class, () -> saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(VISIT_ID_1));
    }

    @Test
    public void shouldReturnNullWhenNoSaleOrderExistsWithVisitId() throws MalformedURLException, XmlRpcException {
        // Setup
        Object[] saleOrders = {};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(saleOrders);

        // Act
        SaleOrder result = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(VISIT_ID_1);

        // Verify
        assertNull(result);
    }

    @Test
    public void shouldThrowErrorWhenNullResponseFromClient() throws MalformedURLException, XmlRpcException {

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(null);

        // Verify
        assertThrows(EIPException.class, () -> saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(VISIT_ID_1));
    }

    public Map<String, Object> getSaleOrderMap(int id, String clientOrderRef, String state, int partnerId) {
        Map<String, Object> saleOrderMap = new HashMap<>();
        saleOrderMap.put("id", id);
        saleOrderMap.put("client_order_ref", clientOrderRef);
        saleOrderMap.put("state", state);
        saleOrderMap.put("partner_id", partnerId);
        return saleOrderMap;
    }
}
