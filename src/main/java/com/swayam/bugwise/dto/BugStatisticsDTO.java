package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.BugStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BugStatisticsDTO {
    private BugStatus status;
    private Long count;
}
