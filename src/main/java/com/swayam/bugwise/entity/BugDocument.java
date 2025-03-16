package com.swayam.bugwise.entity;

import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "bugs")
@Getter
@Setter
@NoArgsConstructor
public class BugDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Keyword)
    private BugStatus status;

    @Field(type = FieldType.Keyword)
    private BugSeverity severity;
}
