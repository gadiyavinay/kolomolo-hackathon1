package com.frauddetection.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class FraudDetectionResult {
    
    private String transactionId;
    private String workflowId;
    private FraudStatus status;
    private BigDecimal riskScore;
    private String decision; // APPROVED, DENIED, CHALLENGED
    private String reason;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;
    
    private Map<String, Object> ruleResults;
    private Map<String, Object> enrichedData;
    private boolean requiresInvestigation;
    
    // Constructors
    public FraudDetectionResult() {
        this.processedAt = LocalDateTime.now();
    }
    
    public FraudDetectionResult(String transactionId, String workflowId) {
        this();
        this.transactionId = transactionId;
        this.workflowId = workflowId;
    }

    // Getters and Setters
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public FraudStatus getStatus() {
        return status;
    }

    public void setStatus(FraudStatus status) {
        this.status = status;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Map<String, Object> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(Map<String, Object> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public Map<String, Object> getEnrichedData() {
        return enrichedData;
    }

    public void setEnrichedData(Map<String, Object> enrichedData) {
        this.enrichedData = enrichedData;
    }

    public boolean isRequiresInvestigation() {
        return requiresInvestigation;
    }

    public void setRequiresInvestigation(boolean requiresInvestigation) {
        this.requiresInvestigation = requiresInvestigation;
    }

    @Override
    public String toString() {
        return "FraudDetectionResult{" +
                "transactionId='" + transactionId + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", status=" + status +
                ", riskScore=" + riskScore +
                ", decision='" + decision + '\'' +
                ", reason='" + reason + '\'' +
                ", processedAt=" + processedAt +
                ", requiresInvestigation=" + requiresInvestigation +
                '}';
    }
}
