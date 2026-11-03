package com.opspilot.workflow.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_wf_version", columnNames = {"workflow_definition_id", "version_number"}))
public class WorkflowVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_definition_id", nullable = false)
    private UUID workflowDefinitionId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "graph_definition_json", nullable = false, columnDefinition = "TEXT")
    private String graphDefinitionJson;

    @Column(name = "validation_result_json", columnDefinition = "TEXT")
    private String validationResultJson;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public WorkflowVersion() {}

    public WorkflowVersion(UUID workflowDefinitionId, Integer versionNumber, String graphDefinitionJson, UUID publishedBy) {
        this.workflowDefinitionId = workflowDefinitionId;
        this.versionNumber = versionNumber;
        this.graphDefinitionJson = graphDefinitionJson;
        this.publishedBy = publishedBy;
        this.publishedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkflowDefinitionId() {
        return workflowDefinitionId;
    }

    public void setWorkflowDefinitionId(UUID workflowDefinitionId) {
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getGraphDefinitionJson() {
        return graphDefinitionJson;
    }

    public void setGraphDefinitionJson(String graphDefinitionJson) {
        this.graphDefinitionJson = graphDefinitionJson;
    }

    public String getValidationResultJson() {
        return validationResultJson;
    }

    public void setValidationResultJson(String validationResultJson) {
        this.validationResultJson = validationResultJson;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(UUID publishedBy) {
        this.publishedBy = publishedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
