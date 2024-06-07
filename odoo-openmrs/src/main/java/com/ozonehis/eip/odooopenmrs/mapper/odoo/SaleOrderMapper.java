/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.mapper.odoo;

import com.ozonehis.eip.odooopenmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.stereotype.Component;

@Component
public class SaleOrderMapper implements ToOdooMapping<Encounter, SaleOrder> {

    @Override
    public SaleOrder toOdoo(Encounter encounter) {
        SaleOrder saleOrder = new SaleOrder();
        if (encounter == null) {
            return null;
        }
        if (encounter.hasPartOf()) {
            String encounterVisitUuid = encounter.getPartOf().getReference().split("/")[1];
            saleOrder.setOrderClientOrderRef(encounterVisitUuid);
            saleOrder.setOrderTypeName("Sales Order");
            saleOrder.setOrderState("draft"); // Default value is always `draft`
        } else {
            throw new IllegalArgumentException(
                    "The Encounter does not have a partOf reference. Cannot map to Sale Order.");
        }
        return saleOrder;
    }
}
