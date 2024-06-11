/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleOrderLine implements OdooDocument {

    @JsonProperty("id")
    private Integer saleOrderLineId;

    @JsonProperty("name")
    private String saleOrderLineName; // Stores Description

    @JsonProperty("order_id")
    private Object saleOrderLineOrderId;

    @JsonProperty("product_id")
    private Object saleOrderLineProductId;

    @JsonProperty("product_uom_qty")
    private Float saleOrderLineProductUomQty;

    @JsonProperty("product_uom")
    private Object saleOrderLineProductUom; // Can be used as a list or Integer, Stores code of product or product name
}
