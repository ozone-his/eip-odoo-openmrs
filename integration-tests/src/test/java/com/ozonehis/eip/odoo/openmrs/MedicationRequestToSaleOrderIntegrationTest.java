/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.model.Partner;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import com.ozonehis.eip.odooopenmrs.routes.partner.CreatePartnerRoute;
import com.ozonehis.eip.odooopenmrs.routes.partner.DeletePartnerRoute;
import com.ozonehis.eip.odooopenmrs.routes.partner.UpdatePartnerRoute;
import com.ozonehis.eip.odooopenmrs.routes.saleorder.CreateSaleOrderRoute;
import com.ozonehis.eip.odooopenmrs.routes.saleorder.DeleteSaleOrderRoute;
import com.ozonehis.eip.odooopenmrs.routes.saleorder.UpdateSaleOrderRoute;
import com.ozonehis.eip.odooopenmrs.routes.saleorderline.CreateSaleOrderLineRoute;
import com.ozonehis.eip.odooopenmrs.routes.saleorderline.DeleteSaleOrderLineRoute;
import com.ozonehis.eip.odooopenmrs.routes.saleorderline.UpdateSaleOrderLineRoute;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.annotations.RouteFixture;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MedicationRequestToSaleOrderIntegrationTest extends BaseRouteIntegrationTest {

    private Bundle medicationRequestBundle;

    private static final String ENCOUNTER_PART_OF_UUID = "26616e46-2cfe-4563-afaa-c243ca94f4c7";

    private static final String PATIENT_UUID = "79355a93-3a4f-4490-98aa-278f922fa87c";

    @BeforeEach
    public void initializeData() {
        medicationRequestBundle = loadResource("fhir.bundle/medication-request-bundle.json", new Bundle());
    }

    @RouteFixture
    public void createRouteBuilder(CamelContext context) throws Exception {
        context = getContextWithRouting(context);

        context.addRoutes(new CreatePartnerRoute());
        context.addRoutes(new UpdatePartnerRoute());
        context.addRoutes(new DeletePartnerRoute());
        context.addRoutes(new CreateSaleOrderRoute());
        context.addRoutes(new UpdateSaleOrderRoute());
        context.addRoutes(new DeleteSaleOrderRoute());
        context.addRoutes(new CreateSaleOrderLineRoute());
        context.addRoutes(new UpdateSaleOrderLineRoute());
        context.addRoutes(new DeleteSaleOrderLineRoute());
    }

    @Test
    @DisplayName("Should verify has sale routes.")
    public void shouldVerifySaleOrderRoutes() {
        assertTrue(hasRoute(contextExtension.getContext(), "medication-request-to-sale-order-router"));
        assertTrue(hasRoute(contextExtension.getContext(), "medication-request-to-sale-order-processor"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-create-sale-order-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-update-sale-order-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-delete-sale-order-route"));
    }

    @Test
    @DisplayName("Should create sale order in Odoo given medication request bundle.")
    public void shouldCreateSaleOrderInOdooGivenMedicationRequestBundle()
            throws MalformedURLException, XmlRpcException {
        // Act
        var headers = new HashMap<String, Object>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        sendBodyAndHeaders("direct:medication-request-to-sale-order-processor", medicationRequestBundle, headers);

        // Verify sale order created
        Object[] result = getOdooClient()
                .searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", ENCOUNTER_PART_OF_UUID), asList("state", "=", "draft")),
                        Constants.orderDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        SaleOrder createdSaleOrder = OdooUtils.convertToObject((Map<String, Object>) result[0], SaleOrder.class);

        assertNotNull(createdSaleOrder);
        assertEquals(ENCOUNTER_PART_OF_UUID, createdSaleOrder.getOrderClientOrderRef());
        assertEquals("draft", createdSaleOrder.getOrderState());

        // verify sale order has sale order line
        assertFalse(createdSaleOrder.getOrderLine().isEmpty());
        assertEquals(1, createdSaleOrder.getOrderLine().size());

        result = getOdooClient()
                .searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        List.of(asList(
                                "id", "=", createdSaleOrder.getOrderLine().get(0))),
                        null);

        assertNotNull(result);
        assertNotNull(result[0]);

        SaleOrderLine createdSaleOrderLine =
                OdooUtils.convertToObject((Map<String, Object>) result[0], SaleOrderLine.class);

        assertNotNull(createdSaleOrderLine);
        assertEquals(
                "Aspirin 81mg | 20.0 Tablet | 2.0 Tablet - Oral - Twice daily - 5 day | Orderer: Super User (Identifier: admin)",
                createdSaleOrderLine.getSaleOrderLineName());

        // Verify partner created
        result = getOdooClient()
                .searchAndRead(
                        Constants.PARTNER_MODEL,
                        List.of(asList("ref", "=", PATIENT_UUID)),
                        Constants.partnerDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        Partner createdPartner = OdooUtils.convertToObject((Map<String, Object>) result[0], Partner.class);

        assertNotNull(createdPartner);
        assertEquals("Jane Doe", createdPartner.getPartnerName());
        assertEquals(PATIENT_UUID, createdPartner.getPartnerRef());
        assertEquals("Tororo", createdPartner.getPartnerCity());
    }

    @Test
    @DisplayName("Should cancel sale order in Odoo given medication request bundle when medication discontinued")
    public void shouldCancelSaleOrderInOdooGivenMedicationRequestBundle()
            throws MalformedURLException, XmlRpcException {
        // Act
        var headers = new HashMap<String, Object>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "d");
        sendBodyAndHeaders("direct:medication-request-to-sale-order-processor", medicationRequestBundle, headers);

        // Verify sale order cancelled
        Object[] result = getOdooClient()
                .searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(
                                asList("client_order_ref", "=", ENCOUNTER_PART_OF_UUID),
                                asList("state", "=", "cancel")),
                        Constants.orderDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        SaleOrder updatedSaleOrder = OdooUtils.convertToObject((Map<String, Object>) result[0], SaleOrder.class);

        assertNotNull(updatedSaleOrder);
        assertEquals(ENCOUNTER_PART_OF_UUID, updatedSaleOrder.getOrderClientOrderRef());
        assertEquals("cancel", updatedSaleOrder.getOrderState());

        // verify sale order has no sale order line
        assertTrue(updatedSaleOrder.getOrderLine().isEmpty());
    }
}
