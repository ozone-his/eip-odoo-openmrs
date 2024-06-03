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

    @JsonProperty("client_order_ref")
    private String orderClientOrderRef;

    @JsonProperty("state")
    private String orderState;

    @JsonProperty("partner_id")
    private Object orderPartnerId; // Can be used as a list or Integer

    @JsonProperty("order_line")
    private List<Integer> orderLine;

    @JsonProperty("type_name")
    private String orderTypeName;
}
