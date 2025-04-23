package com.swayam.bugwise.dto;

import lombok.Data;

@Data
public class OrganizationStatsDTO {
    private String organizationId;
    private String organizationName;
    private int projectCount;
    private int memberCount;
}