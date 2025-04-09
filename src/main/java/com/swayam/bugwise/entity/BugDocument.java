package com.swayam.bugwise.entity;

import com.swayam.bugwise.enums.BugSeverity;
import com.swayam.bugwise.enums.BugStatus;
import com.swayam.bugwise.enums.BugType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.ValueConverter;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "bugs", createIndex = true)
@Getter
@Setter
@NoArgsConstructor
public class BugDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "english")
    private String title;

    @Field(type = FieldType.Text, analyzer = "english")
    private String description;

    @Field(type = FieldType.Keyword)
    private BugStatus status;

    @Field(type = FieldType.Keyword)
    private BugSeverity severity;

    @Field(type = FieldType.Keyword)
    private String projectId;

    @Field(type = FieldType.Text)
    private String projectName;

    @Field(type = FieldType.Keyword)
    private String assignedDeveloperId;

    @Field(type = FieldType.Text)
    private String assignedDeveloperEmail;

    @Field(type = FieldType.Keyword)
    private String reportedById;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updatedAt;

    @Field(type = FieldType.Object)
    private OrganizationRef organization;

    @Field(type = FieldType.Keyword)
    private BugType bugType;

    @Field(type = FieldType.Integer)
    private Integer expectedTimeHours;

    @Field(type = FieldType.Integer)
    private Integer actualTimeHours;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrganizationRef {
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Text)
        private String name;
    }
}