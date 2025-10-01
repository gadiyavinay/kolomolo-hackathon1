package com.frauddetection.model;

public enum FraudStatus {
    PENDING("Transaction is being processed"),
    IN_PROGRESS("Fraud detection in progress"),
    COMPLETED("Fraud detection completed"),
    APPROVED("Transaction approved"),
    DENIED("Transaction denied due to fraud"),
    CHALLENGED("Transaction requires additional verification"),
    INVESTIGATION_REQUIRED("Transaction flagged for investigation"),
    ERROR("Error occurred during processing");

    private final String description;

    FraudStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCompleted() {
        return this == APPROVED || this == DENIED || this == CHALLENGED;
    }

    public boolean requiresAction() {
        return this == CHALLENGED || this == INVESTIGATION_REQUIRED;
    }
}
