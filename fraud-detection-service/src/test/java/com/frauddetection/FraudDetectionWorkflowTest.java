package com.frauddetection;

import com.frauddetection.activity.FraudDetectionActivities;
import com.frauddetection.model.FraudDetectionResult;
import com.frauddetection.model.FraudStatus;
import com.frauddetection.model.TransactionData;
import com.frauddetection.workflow.FraudDetectionWorkflowImpl;
import io.temporal.testing.TestWorkflowRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionWorkflowTest {

    @Mock
    private FraudDetectionActivities mockActivities;

    private final TestWorkflowRule testWorkflowRule = TestWorkflowRule.newBuilder()
            .setWorkflowTypes(FraudDetectionWorkflowImpl.class)
            .setActivityImplementations(mockActivities)
            .build();

    @Test
    void testSafeTransaction() {
        // Setup
        TransactionData safeTransaction = createSafeTransaction();
        Map<String, Object> enrichedData = createMockEnrichedData();
        Map<String, Object> ruleResults = createSafeRuleResults();
        BigDecimal lowRiskScore = new BigDecimal("25.0");
        
        // Mock activity responses
        when(mockActivities.initiateTransaction(any())).thenReturn(safeTransaction);
        when(mockActivities.enrichTransactionData(any())).thenReturn(enrichedData);
        when(mockActivities.runFraudEngineRules(any(), any())).thenReturn(ruleResults);
        when(mockActivities.calculateRiskScore(any(), any())).thenReturn(lowRiskScore);
        when(mockActivities.makeAuthorizationDecision(any(), any(), any())).thenReturn("APPROVED");
        
        // Execute workflow
        FraudDetectionWorkflowImpl workflow = testWorkflowRule.getWorkflowClient()
                .newWorkflowStub(FraudDetectionWorkflowImpl.class);
        
        FraudDetectionResult result = workflow.processFraudDetection(safeTransaction);
        
        // Assertions
        assertNotNull(result);
        assertEquals("APPROVED", result.getDecision());
        assertEquals(FraudStatus.APPROVED, result.getStatus());
        assertEquals(lowRiskScore, result.getRiskScore());
        assertFalse(result.isRequiresInvestigation());
        
        // Verify all activities were called
        verify(mockActivities).initiateTransaction(safeTransaction);
        verify(mockActivities).enrichTransactionData(safeTransaction);
        verify(mockActivities).runFraudEngineRules(eq(safeTransaction), eq(enrichedData));
        verify(mockActivities).calculateRiskScore(eq(safeTransaction), eq(ruleResults));
        verify(mockActivities).makeAuthorizationDecision(eq(safeTransaction), eq(lowRiskScore), eq(ruleResults));
        verify(mockActivities).logTransactionForMonitoring(eq(safeTransaction), eq("APPROVED"), eq(lowRiskScore));
    }

    @Test
    void testRiskyTransaction() {
        // Setup
        TransactionData riskyTransaction = createRiskyTransaction();
        Map<String, Object> enrichedData = createMockEnrichedData();
        Map<String, Object> ruleResults = createRiskyRuleResults();
        BigDecimal highRiskScore = new BigDecimal("65.0");
        
        // Mock activity responses
        when(mockActivities.initiateTransaction(any())).thenReturn(riskyTransaction);
        when(mockActivities.enrichTransactionData(any())).thenReturn(enrichedData);
        when(mockActivities.runFraudEngineRules(any(), any())).thenReturn(ruleResults);
        when(mockActivities.calculateRiskScore(any(), any())).thenReturn(highRiskScore);
        when(mockActivities.makeAuthorizationDecision(any(), any(), any())).thenReturn("CHALLENGED");
        
        // Execute workflow
        FraudDetectionWorkflowImpl workflow = testWorkflowRule.getWorkflowClient()
                .newWorkflowStub(FraudDetectionWorkflowImpl.class);
        
        FraudDetectionResult result = workflow.processFraudDetection(riskyTransaction);
        
        // Assertions
        assertNotNull(result);
        assertEquals("CHALLENGED", result.getDecision());
        assertEquals(FraudStatus.CHALLENGED, result.getStatus());
        assertEquals(highRiskScore, result.getRiskScore());
        assertFalse(result.isRequiresInvestigation());
        
        // Verify activities were called but not investigation
        verify(mockActivities).initiateTransaction(riskyTransaction);
        verify(mockActivities).enrichTransactionData(riskyTransaction);
        verify(mockActivities).runFraudEngineRules(eq(riskyTransaction), eq(enrichedData));
        verify(mockActivities).calculateRiskScore(eq(riskyTransaction), eq(ruleResults));
        verify(mockActivities).makeAuthorizationDecision(eq(riskyTransaction), eq(highRiskScore), eq(ruleResults));
        verify(mockActivities).logTransactionForMonitoring(eq(riskyTransaction), eq("CHALLENGED"), eq(highRiskScore));
        verify(mockActivities, never()).initiateInvestigation(any(), any());
    }

    @Test
    void testFraudulentTransactionWithInvestigation() {
        // Setup
        TransactionData fraudTransaction = createFraudulentTransaction();
        Map<String, Object> enrichedData = createMockEnrichedData();
        Map<String, Object> ruleResults = createFraudulentRuleResults();
        BigDecimal veryHighRiskScore = new BigDecimal("95.0");
        
        // Mock activity responses
        when(mockActivities.initiateTransaction(any())).thenReturn(fraudTransaction);
        when(mockActivities.enrichTransactionData(any())).thenReturn(enrichedData);
        when(mockActivities.runFraudEngineRules(any(), any())).thenReturn(ruleResults);
        when(mockActivities.calculateRiskScore(any(), any())).thenReturn(veryHighRiskScore);
        when(mockActivities.makeAuthorizationDecision(any(), any(), any())).thenReturn("DENIED");
        
        // Execute workflow
        FraudDetectionWorkflowImpl workflow = testWorkflowRule.getWorkflowClient()
                .newWorkflowStub(FraudDetectionWorkflowImpl.class);
        
        FraudDetectionResult result = workflow.processFraudDetection(fraudTransaction);
        
        // Assertions
        assertNotNull(result);
        assertEquals("DENIED", result.getDecision());
        assertEquals(FraudStatus.INVESTIGATION_REQUIRED, result.getStatus());
        assertEquals(veryHighRiskScore, result.getRiskScore());
        assertTrue(result.isRequiresInvestigation());
        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("High fraud risk detected"));
        
        // Verify investigation and blacklist activities were called
        verify(mockActivities).initiateInvestigation(eq(fraudTransaction), any(String.class));
        verify(mockActivities).updateBlacklist(eq(fraudTransaction), any(String.class));
    }

    // Helper methods to create test data
    private TransactionData createSafeTransaction() {
        TransactionData transaction = new TransactionData();
        transaction.setTransactionId("TXN-SAFE-001");
        transaction.setCustomerId("CUST-1001");
        transaction.setAccountId("ACC-10001");
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setCurrency("USD");
        transaction.setMerchantId("MERCHANT_001");
        transaction.setLocation("New York, NY");
        transaction.setChannel("ONLINE");
        return transaction;
    }
    
    private TransactionData createRiskyTransaction() {
        TransactionData transaction = new TransactionData();
        transaction.setTransactionId("TXN-RISKY-001");
        transaction.setCustomerId("CUST-2001");
        transaction.setAccountId("ACC-20001");
        transaction.setAmount(new BigDecimal("2500.00"));
        transaction.setCurrency("USD");
        transaction.setMerchantId("MERCHANT_003");
        transaction.setLocation("International");
        transaction.setChannel("ONLINE");
        return transaction;
    }
    
    private TransactionData createFraudulentTransaction() {
        TransactionData transaction = new TransactionData();
        transaction.setTransactionId("TXN-FRAUD-001");
        transaction.setCustomerId("CUST-3001");
        transaction.setAccountId("ACC-30001");
        transaction.setAmount(new BigDecimal("10000.00"));
        transaction.setCurrency("USD");
        transaction.setMerchantId("MERCHANT_003");
        transaction.setLocation("Unknown Location");
        transaction.setChannel("ATM");
        return transaction;
    }
    
    private Map<String, Object> createMockEnrichedData() {
        Map<String, Object> enrichedData = new HashMap<>();
        enrichedData.put("customerProfile", Map.of("riskLevel", "LOW", "accountAge", 24));
        enrichedData.put("merchantProfile", Map.of("category", "E_COMMERCE", "riskLevel", "LOW"));
        enrichedData.put("deviceLocation", "New York, NY");
        enrichedData.put("isInternational", false);
        return enrichedData;
    }
    
    private Map<String, Object> createSafeRuleResults() {
        Map<String, Object> ruleResults = new HashMap<>();
        ruleResults.put("highAmountRule", Map.of("triggered", false, "threshold", new BigDecimal("1000")));
        ruleResults.put("velocityRule", Map.of("triggered", false, "maxTransactionsPerHour", 5));
        ruleResults.put("blacklistRule", Map.of("triggered", false));
        ruleResults.put("geographicRule", Map.of("triggered", false));
        ruleResults.put("timeAnomalyRule", Map.of("triggered", false));
        ruleResults.put("mlModelRule", Map.of("triggered", false, "score", 0.3));
        return ruleResults;
    }
    
    private Map<String, Object> createRiskyRuleResults() {
        Map<String, Object> ruleResults = new HashMap<>();
        ruleResults.put("highAmountRule", Map.of("triggered", true, "threshold", new BigDecimal("1000")));
        ruleResults.put("velocityRule", Map.of("triggered", false, "maxTransactionsPerHour", 5));
        ruleResults.put("blacklistRule", Map.of("triggered", false));
        ruleResults.put("geographicRule", Map.of("triggered", true));
        ruleResults.put("timeAnomalyRule", Map.of("triggered", false));
        ruleResults.put("mlModelRule", Map.of("triggered", false, "score", 0.6));
        return ruleResults;
    }
    
    private Map<String, Object> createFraudulentRuleResults() {
        Map<String, Object> ruleResults = new HashMap<>();
        ruleResults.put("highAmountRule", Map.of("triggered", true, "threshold", new BigDecimal("1000")));
        ruleResults.put("velocityRule", Map.of("triggered", true, "maxTransactionsPerHour", 5));
        ruleResults.put("blacklistRule", Map.of("triggered", false));
        ruleResults.put("geographicRule", Map.of("triggered", true));
        ruleResults.put("timeAnomalyRule", Map.of("triggered", true));
        ruleResults.put("mlModelRule", Map.of("triggered", true, "score", 0.9));
        return ruleResults;
    }
}
