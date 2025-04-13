package com.swayam.bugwise.service;

import co.elastic.clients.elasticsearch._types.SortOptions;
import com.swayam.bugwise.dto.BugSuggestionDTO;
import com.swayam.bugwise.entity.Bug;
import com.swayam.bugwise.entity.BugDocument;
import com.swayam.bugwise.entity.User;
import com.swayam.bugwise.enums.BugType;
import com.swayam.bugwise.enums.DeveloperType;
import com.swayam.bugwise.enums.UserRole;
import com.swayam.bugwise.repository.jpa.BugRepository;
import com.swayam.bugwise.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AIAnalysisService {
    private final ChatClient chatClient;
    private final BugRepository bugRepository;
    private final UserRepository userRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    public AIAnalysisService(ChatClient.Builder chatClientBuilder, BugRepository bugRepository, UserRepository userRepository, ElasticsearchOperations elasticsearchOperations) {
        this.chatClient = chatClientBuilder.build();
        this.bugRepository = bugRepository;
        this.userRepository = userRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    private static final String BUG_TYPE_PROMPT = """
        Analyze the following bug report and determine its type from these categories:
        FRONTEND, BACKEND, INTEGRATION, PERFORMANCE, SECURITY, OTHER.
        
        Consider these guidelines:
        - UI/FRONTEND: Issues with visual elements, layouts, buttons, styling
        - BACKEND: Server-side logic, business rules, calculations, Endpoints, request/response formats
        - INTEGRATION: Communication between services/components
        - PERFORMANCE: Slow responses, high resource usage
        - SECURITY: Vulnerabilities, authentication, authorization
        
        Bug Title: {title}
        Bug Description: {description}
        
        Respond ONLY with one of the category names (FRONTEND, BACKEND, INTEGRATION, PERFORMANCE, SECURITY, OTHER).
        """;

    private static final String DEVELOPER_TYPE_PROMPT = """
        Based on the bug type '{bugType}', determine which developer types are needed to fix it.
        Choose from: BACKEND, FRONTEND, FULL_STACK, OTHER.
        
        Rules:
        - For UI/FRONTEND bugs: FRONTEND or FULL_STACK
        - For BACKEND/API bugs: BACKEND or FULL_STACK
        - For INTEGRATION bugs: Include both FRONTEND and BACKEND if UI is involved, otherwise BACKEND
        - For PERFORMANCE/SECURITY: FULL_STACK
        - For complex bugs that span multiple areas: Include all relevant types
        
        Respond with a COMMA-SEPARATED LIST of developer types (e.g., "BACKEND,FRONTEND").
        """;

    private static final String TIME_ESTIMATE_PROMPT = """
        Estimate the time required to fix this bug based on:
        - Bug type: {bugType}
        - Title: {title}
        - Description: {description}
        - Severity: {severity}
        
        Consider:
        - Simple UI fixes: 2-4 hours
        - Medium complexity backend: 4-8 hours
        - Complex integrations: 8-16 hours
        - Critical security issues: 16+ hours
        
        Respond ONLY with the estimated hours as a whole number (e.g., "4").
        """;

    private static final String DEVELOPER_SUGGESTION_PROMPT = """
        Suggest developers for a bug with these characteristics:
        - Type: {bugType}
        - Title: {title}
        - Description: {description}
        
        Available developers and their specializations:
        {developersList}
        
        Also consider these similar past bugs and who fixed them:
        {pastBugsInfo}
        
        Respond with a COMMA-SEPARATED LIST of developer IDs in order of suitability.
        """;

    public BugSuggestionDTO getBugSuggestions(String bugId) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new NoSuchElementException("Bug not found"));

        BugType bugType = determineBugTypeWithAI(bug.getTitle(), bug.getDescription());
        Set<DeveloperType> requiredTypes = determineRequiredDeveloperTypesWithAI(bugType);
        int estimatedTime = estimateTimeToFixWithAI(bug, bugType);
        List<User> suggestedDevelopers = getDeveloperSuggestions(bug, requiredTypes);

        BugSuggestionDTO suggestion = new BugSuggestionDTO();
        suggestion.setBugId(bugId);
        suggestion.setSuggestedBugType(bugType);
        suggestion.setRequiredDeveloperTypes(requiredTypes);
        suggestion.setEstimatedTimeHours(estimatedTime);

        List<BugSuggestionDTO.DeveloperSuggestionDTO> developerDTOs = suggestedDevelopers.stream()
                .map(dev -> {
                    BugSuggestionDTO.DeveloperSuggestionDTO dto = new BugSuggestionDTO.DeveloperSuggestionDTO();
                    dto.setUserId(dev.getId());
                    dto.setEmail(dev.getEmail());
                    dto.setDeveloperType(dev.getDeveloperType());
                    return dto;
                })
                .collect(Collectors.toList());

        suggestion.setSuggestedDevelopers(developerDTOs);

        return suggestion;
    }

    private List<User> getDeveloperSuggestions(Bug bug, Set<DeveloperType> requiredTypes) {
        List<User> allDevelopers = userRepository.findByAssignedProjectsIdAndRole(
                bug.getProject().getId(),
                UserRole.DEVELOPER
        );

        List<BugDocument> similarBugs = findSimilarBugs(bug);

        List<String> suggestedDeveloperIds = getAISuggestedDevelopers(
                bug,
                allDevelopers,
                similarBugs
        );

        return suggestedDeveloperIds.stream()
                .map(id -> allDevelopers.stream()
                        .filter(d -> d.getId().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private BugType determineBugTypeWithAI(String title, String description) {
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(BUG_TYPE_PROMPT);
        Prompt prompt = promptTemplate.create(Map.of(
                "title", title,
                "description", description
        ));

        String response = chatClient.prompt(prompt).call().content();
        try {
            return BugType.valueOf(response.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("AI returned invalid bug type: {}", response);
            return BugType.OTHER;
        }
    }

    private Set<DeveloperType> determineRequiredDeveloperTypesWithAI(BugType bugType) {
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(DEVELOPER_TYPE_PROMPT);
        Prompt prompt = promptTemplate.create(Map.of(
                "bugType", bugType
        ));

        String response = chatClient.prompt(prompt).call().content();
        return Arrays.stream(response.split(","))
                .map(String::trim)
                .map(s -> {
                    try {
                        return DeveloperType.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("AI returned invalid developer type: {}", s);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private int estimateTimeToFixWithAI(Bug bug, BugType bugType) {
        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(TIME_ESTIMATE_PROMPT);
        log.info("bug type: {}", bugType);
        Prompt prompt = promptTemplate.create(Map.of(
                "bugType", bugType,
                "title", bug.getTitle(),
                "description", bug.getDescription(),
                "severity", bug.getSeverity()
        ));

        String response = chatClient.prompt(prompt).call().content();
        try {
            return Integer.parseInt(response.trim());
        } catch (NumberFormatException e) {
            log.warn("AI returned invalid time estimate: {}", response);
            return 8;
        }
    }

    private List<String> getAISuggestedDevelopers(Bug bug, List<User> developers, List<BugDocument> similarBugs) {
        log.info("developers: {}", developers);
        String developersList = developers.stream()
                .map(d -> String.format("- %s (%s): %s | Current workload: %d bugs",
                        d.getId(),
                        d.getDeveloperType(),
                        d.getEmail(),
                        d.getAssignedBugs().size()))
                .collect(Collectors.joining("\n"));


        String pastBugsInfo = similarBugs.stream()
                .filter(b -> b.getAssignedDeveloperId() != null)
                .map(b -> {
                    String developersInfo = Arrays.stream(b.getAssignedDeveloperId().split(","))
                            .map(id -> {
                                User dev = developers.stream()
                                        .filter(d -> d.getId().equals(id.trim()))
                                        .findFirst()
                                        .orElse(null);
                                return dev != null ? dev.getEmail() : "Unknown developer";
                            })
                            .collect(Collectors.joining(", "));
                    return String.format("- Similar bug '%s' was fixed by %s in %d hours",
                            b.getTitle(),
                            developersInfo,
                            b.getActualTimeHours() != null ? b.getActualTimeHours() : 0);
                })
                .collect(Collectors.joining("\n"));

        if (pastBugsInfo.isEmpty()) {
            pastBugsInfo = "No similar past bugs found";
        }

        SystemPromptTemplate promptTemplate = new SystemPromptTemplate(DEVELOPER_SUGGESTION_PROMPT);
        Prompt prompt = promptTemplate.create(Map.of(
                "bugType", bug.getBugType() != null ? bug.getBugType().name() : "UNKNOWN",
                "title", bug.getTitle() != null ? bug.getTitle() : "",
                "description", bug.getDescription() != null ? bug.getDescription() : "",
                "developersList", developersList != null ? developersList : "",
                "pastBugsInfo", pastBugsInfo != null ? pastBugsInfo : "No information available"
        ));

        String response = chatClient.prompt(prompt).call().content();
        return Arrays.stream(response.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private List<BugDocument> findSimilarBugs(Bug bug) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .bool(b -> b
                                .must(m -> m.multiMatch(mm -> mm
                                        .fields("title", "description")
                                        .query(bug.getTitle())
                                ))
                                .mustNot(m -> m.term(t -> t
                                        .field("id")
                                        .value(bug.getId())
                                ))
                        )
                )
                .withSort(SortOptions.of(s -> s.field(f -> f
                        .field("createdAt")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                )))
                .withMaxResults(5)
                .build();

        return elasticsearchOperations.search(query, BugDocument.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}