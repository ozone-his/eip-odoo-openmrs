package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odooopenmrs.model.Product;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import com.ozonehis.eip.odooopenmrs.model.Uom;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
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

    private static final String PRODUCT_UOM_ID = "16";

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

    @Test
    public void shouldReturnNullWhenProductAndSaleOrderLineExistsAndResourceIsServiceRequest()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Resource resource = new ServiceRequest();
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(ORDER_ID);
        Product product = new Product();
        product.setProductResId(PRODUCT_ID);
        SaleOrderLine saleOrderLine = getSaleOrderLine();
        Map<String, Object> saleOrderLineMap =
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID);
        Object[] saleOrderLines = {saleOrderLineMap};

        // Mock behaviour
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);
        when(saleOrderLineMapper.toOdoo(resource)).thenReturn(saleOrderLine);

        // Act
        SaleOrderLine result = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);

        // Verify
        assertNull(result);
    }

    @Test
    public void shouldReturnSaleOrderLineWhenProductExistsAndResourceIsServiceRequest()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Resource resource = new ServiceRequest();
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(ORDER_ID);
        Product product = new Product();
        product.setProductResId(PRODUCT_ID);
        SaleOrderLine saleOrderLine = getSaleOrderLine();

        // Mock behaviour
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(new Object[] {});
        when(saleOrderLineMapper.toOdoo(resource)).thenReturn(saleOrderLine);

        // Act
        SaleOrderLine result = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);

        // Verify
        assertNotNull(result);
        assertEquals(5.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(1, saleOrderLine.getSaleOrderLineProductUom());
    }

    @Test
    public void shouldReturnSaleOrderLineWhenProductExistsAndResourceIsMedicationRequest()
            throws MalformedURLException, XmlRpcException {
        // Setup
        Resource resource = new MedicationRequest();
        Uom uom = new Uom();
        uom.setUomResId(999);
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(ORDER_ID);
        Product product = new Product();
        product.setProductResId(PRODUCT_ID);
        SaleOrderLine saleOrderLine = getSaleOrderLine();

        // Mock behaviour
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(new Object[] {});
        when(saleOrderLineMapper.toOdoo(resource)).thenReturn(saleOrderLine);
        when(uomHandler.getUom((String) saleOrderLine.getSaleOrderLineProductUom()))
                .thenReturn(uom);

        // Act
        SaleOrderLine result = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);

        // Verify
        assertNotNull(result);
        assertEquals(5.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(999, saleOrderLine.getSaleOrderLineProductUom());
    }

    public Map<String, Object> getSaleOrderLineMap(
            int id, String name, int orderId, int productId, float productUomQty, String productUom) {
        Map<String, Object> saleOrderMap = new HashMap<>();
        saleOrderMap.put("id", id);
        saleOrderMap.put("name", name);
        saleOrderMap.put("order_id", orderId);
        saleOrderMap.put("product_id", productId);
        saleOrderMap.put("product_uom_qty", productUomQty);
        saleOrderMap.put("product_uom", productUom);
        return saleOrderMap;
    }

    public SaleOrderLine getSaleOrderLine() {
        return OdooUtils.convertToObject(
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID),
                SaleOrderLine.class);
    }
}
