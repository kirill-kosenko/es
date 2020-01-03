package com.transportexchangegroup.replication.test.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLoadCommand extends Command {
    private String status;
    private Details from;
    private Details to;
}
