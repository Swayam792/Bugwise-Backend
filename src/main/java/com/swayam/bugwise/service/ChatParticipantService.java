package com.swayam.bugwise.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ChatParticipantService {
    private final ConcurrentMap<String, Set<String>> bugParticipants = new ConcurrentHashMap<>();

    public void addParticipant(String bugId, String username) {
        bugParticipants.computeIfAbsent(bugId, k -> ConcurrentHashMap.newKeySet())
                .add(username);
    }

    public void removeParticipant(String bugId, String username) {
        Set<String> participants = bugParticipants.get(bugId);
        if (participants != null) {
            participants.remove(username);
            if (participants.isEmpty()) {
                bugParticipants.remove(bugId);
            }
        }
    }

    public Set<String> getCurrentParticipantsForBug(String bugId) {
        return new HashSet<>(bugParticipants.getOrDefault(bugId, Collections.emptySet()));
    }

}