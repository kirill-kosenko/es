package com.transportexchangegroup.replication.test.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Details {
    private String timeZoneName;
    private String fullAddress;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant timeTo;
    private String area;
    private String timeRange;
    private Integer countryId;
    private String town;
    private String building;
    private LatLng location;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private Instant timeFrom;
}
