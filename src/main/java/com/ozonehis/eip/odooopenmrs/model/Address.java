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
public class Address implements OdooDocument {
    @Nonnull
    @JsonProperty("street")
    private String addressStreet;

    @Nonnull
    @JsonProperty("street2")
    private String addressStreet2;

    @Nonnull
    @JsonProperty("zip")
    private String addressZip;

    @Nonnull
    @JsonProperty("city")
    private String addressCity;

    @Nonnull
    @JsonProperty("contact_address")
    private String addressContactAddress;

    private Country country;

    private CountryState countryState;
}
