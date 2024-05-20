/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.handlers;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SaleOrderLineHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    public Optional<SaleOrderLine> saleOrderLineExists(String name) {
        List<List<List<Object>>> searchQuery =
                Collections.singletonList(Collections.singletonList(Arrays.asList("name", "=", name)));
        try {
            Object[] records = (Object[])
                    odooClient.execute(Constants.SEARCH_METHOD, Constants.SALE_ORDER_LINE_MODEL, searchQuery, null);
            if ((records != null) && (records.length > 0)) {
                return Optional.ofNullable((SaleOrderLine) records[0]); // TODO: Fix
            }
        } catch (XmlRpcException e) {
            log.error("Error while checking if sales order line exists with name {} error {}", name, e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<SaleOrderLine> createSaleOrderLineIfItemExists(Resource resource) {
        return getItemName(resource)
                .flatMap(this::saleOrderLineExists)
                .map(item -> saleOrderLineMapper.toOdoo(resource));
    }

    private Optional<String> getItemName(Resource resource) {
        if (resource instanceof ServiceRequest serviceRequest) {
            return Optional.of(serviceRequest.getCode().getCodingFirstRep().getCode());
        } else if (resource instanceof MedicationRequest medicationRequest) {
            return Optional.of(
                    medicationRequest.getMedicationReference().getReference().split("/")[1]);
        } else {
            throw new IllegalArgumentException(
                    "Unsupported resource type: " + resource.getClass().getName());
        }
    }
}
