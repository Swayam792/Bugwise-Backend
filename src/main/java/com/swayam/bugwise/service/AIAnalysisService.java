package com.swayam.bugwise.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AIAnalysisService {

    @Async
    public void analyzeBug(String bugId) {

    }
}