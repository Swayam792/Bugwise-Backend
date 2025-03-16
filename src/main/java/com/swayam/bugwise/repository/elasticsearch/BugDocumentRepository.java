package com.swayam.bugwise.repository.elasticsearch;

import com.swayam.bugwise.entity.BugDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface BugDocumentRepository extends ElasticsearchRepository<BugDocument, String> {
    @Query("{\"bool\": {\"should\": [{\"match_phrase\": {\"title\": \"?0\"}}, {\"match_phrase\": {\"description\": \"?0\"}}]}}")
    List<BugDocument> findByTitleContainingOrDescriptionContaining(String query);
}
