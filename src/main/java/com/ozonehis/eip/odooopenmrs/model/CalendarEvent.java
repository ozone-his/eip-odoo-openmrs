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
public class CalendarEvent implements OdooDocument {

    @Nonnull
    @JsonProperty("active")
    private Boolean calendarEventActive;

    @Nonnull
    @JsonProperty("attendee_ids")
    private Integer[] calendarEventAttendeeIds;

    @Nonnull
    @JsonProperty("create_date")
    private Date calendarEventCreateDate;

    @Nonnull
    @JsonProperty("description")
    private String calendarEventDescription;

    @Nonnull
    @JsonProperty("duration")
    private Float calendarEventDuration;

    @Nonnull
    @JsonProperty("name")
    private String calendarEventName;

    @Nonnull
    @JsonProperty("start")
    private Date calendarEventStart;

    @Nonnull
    @JsonProperty("stop")
    private Date calendarEventStop;
}
