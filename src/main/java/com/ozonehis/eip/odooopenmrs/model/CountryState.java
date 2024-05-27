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
public class CountryState implements OdooDocument {

    @JsonProperty("id")
    private Integer countryStateId;

    @JsonProperty("name")
    private String countryStateName;


    @JsonProperty("code")
    private String countryStateCode; // Check if required
}
