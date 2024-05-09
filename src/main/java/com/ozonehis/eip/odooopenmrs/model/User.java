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
public class User implements OdooDocument {

    @Nonnull
    @JsonProperty("id")
    private Integer userId;

    @Nonnull
    @JsonProperty("login_date")
    private Date userLoginDate;

    @Nonnull
    @JsonProperty("name")
    private String userName;

    @Nonnull
    @JsonProperty("email")
    private String userEmail;
}
