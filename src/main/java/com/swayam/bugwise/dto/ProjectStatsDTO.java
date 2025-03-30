package com.swayam.bugwise.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatsDTO {
    String name;
    int open;
    int inProgress;
    int resolved;
}
