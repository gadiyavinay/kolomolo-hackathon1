package com.frauddetection.activity;

import com.frauddetection.model.TransactionData;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.math.BigDecimal;
import java.util.Map;

@ActivityInterface
public interface FraudDetectionActivities {
    
    @ActivityMethod
    TransactionData initiateTransaction(TransactionData transactionData);
    
    @ActivityMethod
    Map<String, Object> enrichTransactionData(TransactionData transactionData);
    
    @ActivityMethod
    Map<String, Object> runFraudEngineRules(TransactionData transactionData, Map<String, Object> enrichedData);
    
    @ActivityMethod
    BigDecimal calculateRiskScore(TransactionData transactionData, Map<String, Object> ruleResults);
    
    @ActivityMethod
    String makeAuthorizationDecision(TransactionData transactionData, BigDecimal riskScore, Map<String, Object> ruleResults);
    
    @ActivityMethod
    void logTransactionForMonitoring(TransactionData transactionData, String decision, BigDecimal riskScore);
    
    @ActivityMethod
    void initiateInvestigation(TransactionData transactionData, String reason);
    
    @ActivityMethod
    void updateBlacklist(TransactionData transactionData, String reason);
}
