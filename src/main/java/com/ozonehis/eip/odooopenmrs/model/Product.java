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
public class Product implements OdooDocument {

    @Nonnull
    @JsonProperty("display_name")
    private String ProductDisplayName; // Product Name

    @Nonnull
    @JsonProperty("name")
    private String ProductName; // Product ID
}
