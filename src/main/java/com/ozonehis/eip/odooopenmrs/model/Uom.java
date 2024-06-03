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
public class Uom {

    @JsonProperty("id")
    private Integer uomId;

    @JsonProperty("name")
    private String uomName;

    @JsonProperty("res_id")
    private Integer uomResId;

    @JsonProperty("display_name")
    private String uomDisplayName;
}
