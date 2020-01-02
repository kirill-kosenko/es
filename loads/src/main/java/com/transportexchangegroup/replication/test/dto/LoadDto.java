package com.transportexchangegroup.replication.test.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadDto {
    private Long loadId;
    private String fromAddress;
    private String toAddress;
}
