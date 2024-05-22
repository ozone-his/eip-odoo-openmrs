package com.ozonehis.eip.odooopenmrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleOrder implements OdooDocument {

    @JsonProperty("id")
    private Integer orderId;

    @JsonProperty("name")
    private String orderName;

    @JsonProperty("state")
    private String orderState;

    @JsonProperty("partner_id")
    private int orderPartnerId; // TODO check if list in Odoo

    @JsonProperty("order_line")
    private List<Integer> orderLine;

    @JsonProperty("type_name")
    private String orderTypeName;

    // Not required below

    //    @JsonProperty("date_order")
    //    private Date orderDate;
    //
    //    @JsonProperty("invoice_status")
    //    private String orderInvoiceStatus;
    //
    //    @JsonProperty("title")
    //    private String orderTitle; // TODO: Check in odoo
    //
    //    @JsonProperty("party_name")
    //    private String orderPartyName; // TODO: Check in odoo
    //
    //    @JsonProperty("customer_name")
    //    private String orderPartnerName; // TODO: Check in odoo
    //
    //    @JsonProperty("items")
    //    private Set<SaleOrderLine> saleOrderLines = new HashSet<>(); // TODO: Check in odoo
    //
    //    public void addSaleOrderLines(HashSet<SaleOrderLine> orderLines) {
    //        saleOrderLines.addAll(orderLines);
    //    }
    //
    //    public void addSaleOrderLine(SaleOrderLine orderLine) {
    //        saleOrderLines.add(orderLine);
    //    }
    //
    //    public void removeSaleOrderLine(SaleOrderLine orderLine) {
    //        saleOrderLines.removeIf(line ->
    // line.getSaleOrderLineOrderId().equals(orderLine.getSaleOrderLineOrderId()));
    //    }
    //
    //    public void removeSaleOrderLine(String orderLineId) {
    //        saleOrderLines.removeIf(line -> line.getSaleOrderLineOrderId().equals(orderLineId));
    //    }
    //
    //    public boolean hasSaleOrderLine(SaleOrderLine orderLine) {
    //        return saleOrderLines.stream()
    //                .anyMatch(line -> line.getSaleOrderLineOrderId().equals(orderLine.getSaleOrderLineOrderId()));
    //    }
    //
    //    public boolean hasSaleOrderLines() {
    //        return !saleOrderLines.isEmpty();
    //    }
}
