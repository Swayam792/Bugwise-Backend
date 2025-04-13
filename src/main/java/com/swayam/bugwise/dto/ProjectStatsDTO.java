package com.swayam.bugwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatsDTO {
    private String projectName; 
    private Map<String, Integer> statusCounts;
}
