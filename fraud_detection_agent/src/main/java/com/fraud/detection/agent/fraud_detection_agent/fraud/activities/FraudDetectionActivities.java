package com.fraud.detection.agent.fraud_detection_agent.fraud.activities;

import com.fraud.detection.agent.fraud_detection_agent.fraud.model.Transaction;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FraudDetectionActivities {

    @ActivityMethod
    double scoreTransaction(Transaction transaction); // ML scoring

    @ActivityMethod
    void freezeAccount(String accountId);

    @ActivityMethod
    void notifyUser(String accountId);

    @ActivityMethod
    void logAudit(Transaction transaction, double score);

    void notifyDownstream(Transaction transaction);
}
