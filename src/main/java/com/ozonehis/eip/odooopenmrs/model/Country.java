package com.ozonehis.eip.odooopenmrs.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Country implements OdooDocument {

    @Nonnull
    @JsonProperty("id")
    private Integer countryId;

    @Nonnull
    @JsonProperty("name")
    private String countryName;

    @Nonnull
    @JsonProperty("code")
    private String countryCode;

    @Nonnull
    @JsonProperty("address_format")
    private String countryAddressFormat;

    @Nonnull
    @JsonProperty("phone_code")
    private Integer countryPhoneCode;

    @Nonnull
    @JsonProperty("login_date")
    private Date userLoginDate;
}
