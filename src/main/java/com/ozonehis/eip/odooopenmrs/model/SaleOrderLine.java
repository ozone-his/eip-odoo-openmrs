package com.ozonehis.eip.odooopenmrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

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
    private String saleOrderLineName;

    @Nonnull
    @JsonProperty("order_id")
    private String saleOrderLineOrderId; // TODO: Check if List<Object>

    @Nonnull
    @JsonProperty("qty_invoiced")
    private Float saleOrderLineQuantityInvoiced;

    @Nonnull
    @JsonProperty("invoice_status")
    private String saleOrderLineInvoiceStatus;

    @Nonnull
    @JsonProperty("price_unit")
    private Float saleOrderLinePriceUnit;

    @Nonnull
    @JsonProperty("price_subtotal")
    private Float saleOrderLinePriceSubtotal;

    @Nonnull
    @JsonProperty("price_tax")
    private Float saleOrderLinePriceTax;

    @Nonnull
    @JsonProperty("price_total")
    private Float saleOrderLinePriceTotal;

    @Nonnull
    @JsonProperty("price_reduce")
    private Float saleOrderLinePriceReduce;

    @Nonnull
    @JsonProperty("product_uom")
    private String saleOrderLineProductUom;// TODO: Check if List<Object>

    @Nonnull
    @JsonProperty("scheduled_date")
    private Date saleOrderLineScheduledDate;
}
