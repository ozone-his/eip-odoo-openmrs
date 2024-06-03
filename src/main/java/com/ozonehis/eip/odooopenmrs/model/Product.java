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
public class Product implements OdooDocument {

    @JsonProperty("id")
    private Integer productId;

    @JsonProperty("display_name")
    private String productDisplayName; // Product Name

    @JsonProperty("name")
    private String productName; // Product ID

    @JsonProperty("res_id")
    private Integer productResId;
}
