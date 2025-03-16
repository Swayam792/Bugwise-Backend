package com.swayam.bugwise.dto;

import com.swayam.bugwise.enums.ParticipantAction;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ParticipantsUpdateDTO {
    private String bugId;
    private List<String> participants;
    private String username;
    private ParticipantAction action;
}
