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

    @JsonProperty("type")
    private String partnerType;

    @JsonProperty("street")
    private String partnerStreet;

    @JsonProperty("street2")
    private String partnerStreet2;

    @JsonProperty("city")
    private String partnerCity;

    @JsonProperty("zip")
    private String partnerZip;

    @JsonProperty("country_id")
    private Integer partnerCountryId;

    @JsonProperty("state_id")
    private Integer partnerStateId;

    @JsonProperty("active")
    private Boolean partnerActive;

    @JsonProperty("comment")
    private String partnerComment; // Stores emrIdField
}
