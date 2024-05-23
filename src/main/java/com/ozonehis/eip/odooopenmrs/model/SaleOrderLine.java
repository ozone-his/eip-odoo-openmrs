package com.ozonehis.eip.odooopenmrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleOrderLine implements OdooDocument {

    @Nonnull
    @JsonProperty("id")
    private Integer saleOrderLineId;

    @Nonnull
    @JsonProperty("name")
    private String saleOrderLineName; // Stores Description

    @JsonProperty("order_id")
    private Integer saleOrderLineOrderId; // TODO: Check if List<Object>

    @JsonProperty("product_id")
    private Integer saleOrderLineProductId; // TODO: Check if List<Object>

    @JsonProperty("product_uom_qty")
    private Float saleOrderLineProductUomQty;

    @JsonProperty("product_uom")
    private Object saleOrderLineProductUom; // TODO: Check if List<Object>
}
