/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odoo.openmrs.model.Product;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
    public void shouldReturnSaleOrderWhenOnlyOneSaleOrderExistsWithVisitId() {
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
    public void shouldThrowErrorWhenMultipleSaleOrderExistsWithSameVisitId() {
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
    public void shouldReturnNullWhenNoSaleOrderExistsWithVisitId() {
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
    public void shouldThrowErrorWhenNullResponseFromClient() {
        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(null);

        // Verify
        assertThrows(EIPException.class, () -> saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(VISIT_ID_1));
    }

    @Test
    public void shouldUpdateSaleOrderWithSaleOrderLine() {
        // Setup
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        SaleOrder saleOrder = new SaleOrder();
        Resource resource = new MedicationRequest();

        // Mock behaviour
        when(saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder))
                .thenReturn(saleOrderLine);
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);

        // Act
        saleOrderHandler.updateSaleOrderIfExistsWithSaleOrderLine(resource, saleOrder, VISIT_ID_1, producerTemplate);

        // Verify
        verify(producerTemplate, times(1)).sendBody("direct:odoo-create-sale-order-line-route", saleOrderLine);
    }

    @Test
    public void shouldCreateSaleOrderWithSaleOrderLine() {
        // Setup
        int partnerId = 12;
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        Encounter encounter = new Encounter();
        SaleOrder saleOrder = getSaleOrder();
        Resource resource = new MedicationRequest();
        Map<String, Object> saleOrderMap = getSaleOrderMap(1, VISIT_ID_1, "draft", 12);

        // Mock behaviour
        when(saleOrderMapper.toOdoo(encounter)).thenReturn(saleOrder);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(new Object[] {saleOrderMap});
        when(saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder))
                .thenReturn(saleOrderLine);
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);

        // Act
        saleOrderHandler.createSaleOrderWithSaleOrderLine(resource, encounter, partnerId, VISIT_ID_1, producerTemplate);

        // Verify
        verify(producerTemplate, times(1))
                .sendBodyAndHeaders("direct:odoo-create-sale-order-route", saleOrder, new HashMap<>());
        verify(producerTemplate, times(1)).sendBody("direct:odoo-create-sale-order-line-route", saleOrderLine);
    }

    @Test
    public void shouldDeleteSaleOrderLine() {
        // Setup
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        Product product = new Product();
        product.setProductResId(123);
        SaleOrder saleOrder = getSaleOrder();
        Resource resource = new MedicationRequest();
        Map<String, Object> saleOrderMap = getSaleOrderMap(1, VISIT_ID_1, "draft", 12);

        // Mock behaviour
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(new Object[] {saleOrderMap});
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(saleOrderLineHandler.getSaleOrderLineIfExists(saleOrder.getOrderId(), product.getProductResId()))
                .thenReturn(saleOrderLine);
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);

        // Act
        saleOrderHandler.deleteSaleOrderLine(resource, VISIT_ID_1, producerTemplate);

        // Verify
        verify(saleOrderLineHandler, times(1))
                .sendSaleOrderLine(producerTemplate, "direct:odoo-delete-sale-order-line-route", saleOrderLine);
    }

    @Test
    public void shouldCancelSaleOrderWhenNoSaleOrderLine() {
        // Setup
        int partnerId = 12;
        Product product = new Product();
        product.setProductResId(123);
        SaleOrder saleOrder = getSaleOrder();
        saleOrder.setOrderState("cancel");
        Map<String, Object> saleOrderMap = getSaleOrderMap(1, VISIT_ID_1, "draft", 12);
        Map<String, Object> saleOrderHeaders = new HashMap<>();
        saleOrderHeaders.put(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(saleOrder.getOrderId()));

        // Mock behaviour
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", VISIT_ID_1), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes))
                .thenReturn(new Object[] {saleOrderMap});
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);

        // Act
        saleOrderHandler.cancelSaleOrderWhenNoSaleOrderLine(partnerId, VISIT_ID_1, producerTemplate);

        // Verify
        verify(producerTemplate, times(1))
                .sendBodyAndHeaders("direct:odoo-update-sale-order-route", saleOrder, saleOrderHeaders);
    }

    public Map<String, Object> getSaleOrderMap(int id, String clientOrderRef, String state, int partnerId) {
        Map<String, Object> saleOrderMap = new HashMap<>();
        saleOrderMap.put("id", id);
        saleOrderMap.put("client_order_ref", clientOrderRef);
        saleOrderMap.put("state", state);
        saleOrderMap.put("partner_id", partnerId);
        return saleOrderMap;
    }

    private SaleOrder getSaleOrder() {
        return OdooUtils.convertToObject(getSaleOrderMap(1, VISIT_ID_1, "draft", 12), SaleOrder.class);
    }
}
