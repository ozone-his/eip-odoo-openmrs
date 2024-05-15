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
public class Partner implements OdooDocument {

    @Nonnull
    @JsonProperty("name")
    private String partnerName;

    @Nonnull
    @JsonProperty("ref")
    private String partnerRef;

    @Nonnull
    @JsonProperty("street")
    private String partnerStreet;

    @Nonnull
    @JsonProperty("street2")
    private String partnerStreet2;

    @Nonnull
    @JsonProperty("zip")
    private String partnerZip;

    @Nonnull
    @JsonProperty("city")
    private String partnerCity;

    @Nonnull
    @JsonProperty("contact_address")
    private String partnerContactAddress;

    @Nonnull
    @JsonProperty("email")
    private String partnerEmail;

    @Nonnull
    @JsonProperty("phone")
    private String partnerPhone;

    private Country country;

    private CountryState countryState;
}
