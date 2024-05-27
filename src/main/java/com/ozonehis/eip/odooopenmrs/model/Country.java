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
public class Country implements OdooDocument {

    @JsonProperty("id")
    private Integer countryId;

    @JsonProperty("name")
    private String countryName;

    @JsonProperty("code")
    private String countryCode;
}
