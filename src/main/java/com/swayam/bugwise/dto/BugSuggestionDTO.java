package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.BugType;
import com.swayam.bugwise.enums.DeveloperType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BugSuggestionDTO {
    private String bugId;
    private BugType suggestedBugType;
    private Set<DeveloperType> requiredDeveloperTypes;
    private int estimatedTimeHours;
    private List<DeveloperSuggestionDTO> suggestedDevelopers;

    @Data
    public static class DeveloperSuggestionDTO {
        private String userId;
        private String email;
        private DeveloperType developerType;
    }
}