package com.frauddetection.workflow;

import com.frauddetection.activity.FraudDetectionActivities;
import com.frauddetection.model.FraudDetectionResult;
import com.frauddetection.model.FraudStatus;
import com.frauddetection.model.TransactionData;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

public class FraudDetectionWorkflowImpl implements FraudDetectionWorkflow {
    
    private static final Logger logger = Workflow.getLogger(FraudDetectionWorkflowImpl.class);
    
    // Configure activity options
    private final ActivityOptions activityOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(1))
                    .setMaximumInterval(Duration.ofSeconds(10))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();
    
    private final FraudDetectionActivities activities = Workflow.newActivityStub(
            FraudDetectionActivities.class, activityOptions);

    @Override
    public FraudDetectionResult processFraudDetection(TransactionData transactionData) {
        logger.info("Starting fraud detection workflow for transaction: {}", transactionData.getTransactionId());
        
        String workflowId = Workflow.getInfo().getWorkflowId();
        FraudDetectionResult result = new FraudDetectionResult(transactionData.getTransactionId(), workflowId);
        result.setStatus(FraudStatus.IN_PROGRESS);
        
        try {
            // Step 1: Transaction Initiation
            logger.info("Step 1: Initiating transaction - {}", transactionData.getTransactionId());
            TransactionData initiatedTransaction = activities.initiateTransaction(transactionData);
            
            // Step 2: Data Enrichment
            logger.info("Step 2: Enriching transaction data - {}", transactionData.getTransactionId());
            Map<String, Object> enrichedData = activities.enrichTransactionData(initiatedTransaction);
            result.setEnrichedData(enrichedData);
            
            // Step 3: Fraud Engine Rules
            logger.info("Step 3: Running fraud engine rules - {}", transactionData.getTransactionId());
            Map<String, Object> ruleResults = activities.runFraudEngineRules(initiatedTransaction, enrichedData);
            result.setRuleResults(ruleResults);
            
            // Step 4: Risk Scoring
            logger.info("Step 4: Calculating risk score - {}", transactionData.getTransactionId());
            BigDecimal riskScore = activities.calculateRiskScore(initiatedTransaction, ruleResults);
            result.setRiskScore(riskScore);
            
            // Step 5: Authorization Decision
            logger.info("Step 5: Making authorization decision - {}", transactionData.getTransactionId());
            String decision = activities.makeAuthorizationDecision(initiatedTransaction, riskScore, ruleResults);
            result.setDecision(decision);
            
            // Step 6: Post-transaction Monitoring
            logger.info("Step 6: Logging for post-transaction monitoring - {}", transactionData.getTransactionId());
            activities.logTransactionForMonitoring(initiatedTransaction, decision, riskScore);
            
            // Step 7: Investigation and Blacklist Updates (conditional)
            if ("DENIED".equals(decision) && riskScore.compareTo(new BigDecimal("90")) >= 0) {
                logger.info("Step 7a: Initiating investigation for high-risk denied transaction - {}", 
                           transactionData.getTransactionId());
                String reason = generateInvestigationReason(ruleResults, riskScore);
                activities.initiateInvestigation(initiatedTransaction, reason);
                result.setRequiresInvestigation(true);
                result.setReason(reason);
                result.setStatus(FraudStatus.INVESTIGATION_REQUIRED);
                
                // Update blacklist if extremely high risk
                if (riskScore.compareTo(new BigDecimal("95")) >= 0) {
                    logger.info("Step 7b: Updating blacklist for extremely high-risk transaction - {}", 
                               transactionData.getTransactionId());
                    activities.updateBlacklist(initiatedTransaction, "Extremely high fraud risk score: " + riskScore);
                }
            } else {
                // Set final status based on decision
                switch (decision) {
                    case "APPROVED" -> {
                        result.setStatus(FraudStatus.APPROVED);
                        result.setReason("Transaction passed all fraud checks");
                    }
                    case "DENIED" -> {
                        result.setStatus(FraudStatus.DENIED);
                        result.setReason("Transaction failed fraud checks - Risk score: " + riskScore);
                    }
                    case "CHALLENGED" -> {
                        result.setStatus(FraudStatus.CHALLENGED);
                        result.setReason("Transaction requires additional verification - Risk score: " + riskScore);
                    }
                    default -> {
                        result.setStatus(FraudStatus.ERROR);
                        result.setReason("Unknown decision type: " + decision);
                    }
                }
            }
            
            logger.info("Fraud detection workflow completed successfully for transaction: {} with decision: {}", 
                       transactionData.getTransactionId(), decision);
            
        } catch (Exception e) {
            logger.error("Error in fraud detection workflow for transaction: {}", 
                        transactionData.getTransactionId(), e);
            result.setStatus(FraudStatus.ERROR);
            result.setReason("Workflow execution error: " + e.getMessage());
        }
        
        return result;
    }
    
    private String generateInvestigationReason(Map<String, Object> ruleResults, BigDecimal riskScore) {
        StringBuilder reason = new StringBuilder("High fraud risk detected (Score: " + riskScore + "). ");
        
        if (isRuleTriggered(ruleResults, "blacklistRule")) {
            reason.append("Account on blacklist. ");
        }
        if (isRuleTriggered(ruleResults, "velocityRule")) {
            reason.append("High transaction velocity. ");
        }
        if (isRuleTriggered(ruleResults, "highAmountRule")) {
            reason.append("High transaction amount. ");
        }
        if (isRuleTriggered(ruleResults, "geographicRule")) {
            reason.append("Geographic anomaly detected. ");
        }
        if (isRuleTriggered(ruleResults, "mlModelRule")) {
            reason.append("ML model flagged as high risk. ");
        }
        
        return reason.toString().trim();
    }
    
    private boolean isRuleTriggered(Map<String, Object> ruleResults, String ruleName) {
        Map<String, Object> rule = (Map<String, Object>) ruleResults.get(ruleName);
        return rule != null && (Boolean) rule.get("triggered");
    }
}
