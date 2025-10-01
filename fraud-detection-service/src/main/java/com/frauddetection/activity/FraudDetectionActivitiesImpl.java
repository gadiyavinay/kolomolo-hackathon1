package com.frauddetection.activity;

import com.frauddetection.model.TransactionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class FraudDetectionActivitiesImpl implements FraudDetectionActivities {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionActivitiesImpl.class);
    private final Random random = new Random();
    
    // Mock databases for demo purposes
    private static final Map<String, List<TransactionData>> customerTransactionHistory = new HashMap<>();
    private static final Set<String> blacklistedAccounts = new HashSet<>();
    private static final Map<String, String> merchantInfo = new HashMap<>();
    
    static {
        // Initialize mock merchant data
        merchantInfo.put("MERCHANT_001", "Amazon Online Store");
        merchantInfo.put("MERCHANT_002", "Local Gas Station");
        merchantInfo.put("MERCHANT_003", "International Wire Transfer");
        merchantInfo.put("MERCHANT_004", "Walmart Supermarket");
        merchantInfo.put("MERCHANT_005", "ATM Withdrawal");
    }

    @Override
    public TransactionData initiateTransaction(TransactionData transactionData) {
        logger.info("Initiating transaction: {}", transactionData.getTransactionId());
        
        // Set default values if missing
        if (transactionData.getTimestamp() == null) {
            transactionData.setTimestamp(LocalDateTime.now());
        }
        
        if (transactionData.getChannel() == null) {
            transactionData.setChannel("ONLINE");
        }
        
        // Add to transaction history for future velocity checks
        customerTransactionHistory.computeIfAbsent(transactionData.getCustomerId(), k -> new ArrayList<>())
                .add(transactionData);
        
        logger.info("Transaction initiated successfully: {}", transactionData.getTransactionId());
        return transactionData;
    }

    @Override
    public Map<String, Object> enrichTransactionData(TransactionData transactionData) {
        logger.info("Enriching transaction data for: {}", transactionData.getTransactionId());
        
        Map<String, Object> enrichedData = new HashMap<>();
        
        // Enrich with merchant information
        String merchantName = merchantInfo.getOrDefault(transactionData.getMerchantId(), "Unknown Merchant");
        transactionData.setMerchantName(merchantName);
        
        // Add customer profile data (mocked)
        Map<String, Object> customerProfile = new HashMap<>();
        customerProfile.put("riskLevel", random.nextBoolean() ? "HIGH" : "LOW");
        customerProfile.put("accountAge", random.nextInt(100) + 1);
        customerProfile.put("averageMonthlySpend", new BigDecimal(random.nextInt(5000) + 1000));
        enrichedData.put("customerProfile", customerProfile);
        
        // Add merchant profile data
        Map<String, Object> merchantProfile = new HashMap<>();
        merchantProfile.put("category", getMerchantCategory(transactionData.getMerchantId()));
        merchantProfile.put("riskLevel", random.nextBoolean() ? "MEDIUM" : "LOW");
        enrichedData.put("merchantProfile", merchantProfile);
        
        // Add location data
        enrichedData.put("deviceLocation", getRandomLocation());
        enrichedData.put("isInternational", random.nextBoolean());
        
        logger.info("Transaction data enriched for: {}", transactionData.getTransactionId());
        return enrichedData;
    }

    @Override
    public Map<String, Object> runFraudEngineRules(TransactionData transactionData, Map<String, Object> enrichedData) {
        logger.info("Running fraud engine rules for: {}", transactionData.getTransactionId());
        
        Map<String, Object> ruleResults = new HashMap<>();
        
        // Rule 1: Amount threshold check
        BigDecimal amountThreshold = new BigDecimal("1000");
        boolean highAmount = transactionData.getAmount().compareTo(amountThreshold) > 0;
        ruleResults.put("highAmountRule", Map.of("triggered", highAmount, "threshold", amountThreshold));
        
        // Rule 2: Velocity check
        boolean velocityViolation = checkVelocityViolation(transactionData);
        ruleResults.put("velocityRule", Map.of("triggered", velocityViolation, "maxTransactionsPerHour", 5));
        
        // Rule 3: Blacklist check
        boolean isBlacklisted = blacklistedAccounts.contains(transactionData.getAccountId());
        ruleResults.put("blacklistRule", Map.of("triggered", isBlacklisted));
        
        // Rule 4: Geographic anomaly
        boolean geoAnomaly = (Boolean) enrichedData.getOrDefault("isInternational", false);
        ruleResults.put("geographicRule", Map.of("triggered", geoAnomaly));
        
        // Rule 5: Time-based anomaly (transactions outside business hours)
        boolean timeAnomaly = isOutsideBusinessHours(transactionData.getTimestamp());
        ruleResults.put("timeAnomalyRule", Map.of("triggered", timeAnomaly));
        
        // Rule 6: ML Model scoring (mocked)
        double mlScore = random.nextDouble();
        boolean highRiskMl = mlScore > 0.7;
        ruleResults.put("mlModelRule", Map.of("triggered", highRiskMl, "score", mlScore));
        
        logger.info("Fraud engine rules completed for: {}. Rules triggered: {}", 
                   transactionData.getTransactionId(), countTriggeredRules(ruleResults));
        
        return ruleResults;
    }

    @Override
    public BigDecimal calculateRiskScore(TransactionData transactionData, Map<String, Object> ruleResults) {
        logger.info("Calculating risk score for: {}", transactionData.getTransactionId());
        
        BigDecimal riskScore = BigDecimal.ZERO;
        
        // Weight each rule
        if (isRuleTriggered(ruleResults, "highAmountRule")) {
            riskScore = riskScore.add(new BigDecimal("25"));
        }
        if (isRuleTriggered(ruleResults, "velocityRule")) {
            riskScore = riskScore.add(new BigDecimal("30"));
        }
        if (isRuleTriggered(ruleResults, "blacklistRule")) {
            riskScore = riskScore.add(new BigDecimal("50"));
        }
        if (isRuleTriggered(ruleResults, "geographicRule")) {
            riskScore = riskScore.add(new BigDecimal("20"));
        }
        if (isRuleTriggered(ruleResults, "timeAnomalyRule")) {
            riskScore = riskScore.add(new BigDecimal("15"));
        }
        if (isRuleTriggered(ruleResults, "mlModelRule")) {
            Map<String, Object> mlRule = (Map<String, Object>) ruleResults.get("mlModelRule");
            double mlScore = (Double) mlRule.get("score");
            riskScore = riskScore.add(BigDecimal.valueOf(mlScore * 30));
        }
        
        // Cap at 100
        if (riskScore.compareTo(new BigDecimal("100")) > 0) {
            riskScore = new BigDecimal("100");
        }
        
        logger.info("Risk score calculated for {}: {}", transactionData.getTransactionId(), riskScore);
        return riskScore;
    }

    @Override
    public String makeAuthorizationDecision(TransactionData transactionData, BigDecimal riskScore, Map<String, Object> ruleResults) {
        logger.info("Making authorization decision for: {} with risk score: {}", 
                   transactionData.getTransactionId(), riskScore);
        
        String decision;
        
        if (riskScore.compareTo(new BigDecimal("75")) >= 0) {
            decision = "DENIED";
        } else if (riskScore.compareTo(new BigDecimal("50")) >= 0) {
            decision = "CHALLENGED";
        } else {
            decision = "APPROVED";
        }
        
        logger.info("Authorization decision for {}: {}", transactionData.getTransactionId(), decision);
        return decision;
    }

    @Override
    public void logTransactionForMonitoring(TransactionData transactionData, String decision, BigDecimal riskScore) {
        logger.info("Logging transaction for monitoring - ID: {}, Decision: {}, Risk Score: {}", 
                   transactionData.getTransactionId(), decision, riskScore);
        
        // In a real system, this would write to a monitoring database or stream
        Map<String, Object> monitoringData = new HashMap<>();
        monitoringData.put("transactionId", transactionData.getTransactionId());
        monitoringData.put("customerId", transactionData.getCustomerId());
        monitoringData.put("amount", transactionData.getAmount());
        monitoringData.put("decision", decision);
        monitoringData.put("riskScore", riskScore);
        monitoringData.put("timestamp", LocalDateTime.now());
        
        logger.info("Transaction monitoring data logged: {}", monitoringData);
    }

    @Override
    public void initiateInvestigation(TransactionData transactionData, String reason) {
        logger.warn("Initiating investigation for transaction: {} - Reason: {}", 
                   transactionData.getTransactionId(), reason);
        
        // In a real system, this would create an investigation case
        Map<String, Object> investigationCase = new HashMap<>();
        investigationCase.put("transactionId", transactionData.getTransactionId());
        investigationCase.put("customerId", transactionData.getCustomerId());
        investigationCase.put("reason", reason);
        investigationCase.put("priority", "HIGH");
        investigationCase.put("createdAt", LocalDateTime.now());
        
        logger.warn("Investigation case created: {}", investigationCase);
    }

    @Override
    public void updateBlacklist(TransactionData transactionData, String reason) {
        logger.warn("Adding account to blacklist: {} - Reason: {}", 
                   transactionData.getAccountId(), reason);
        
        blacklistedAccounts.add(transactionData.getAccountId());
        
        logger.warn("Account blacklisted: {} Total blacklisted accounts: {}", 
                   transactionData.getAccountId(), blacklistedAccounts.size());
    }

    // Helper methods
    private boolean checkVelocityViolation(TransactionData transactionData) {
        List<TransactionData> customerHistory = customerTransactionHistory.get(transactionData.getCustomerId());
        if (customerHistory == null) return false;
        
        LocalDateTime oneHourAgo = LocalDateTime.now().minus(1, ChronoUnit.HOURS);
        long recentTransactions = customerHistory.stream()
                .filter(tx -> tx.getTimestamp().isAfter(oneHourAgo))
                .count();
        
        return recentTransactions > 5; // More than 5 transactions in last hour
    }

    private boolean isOutsideBusinessHours(LocalDateTime timestamp) {
        int hour = timestamp.getHour();
        return hour < 9 || hour > 17; // Outside 9 AM - 5 PM
    }

    private String getMerchantCategory(String merchantId) {
        Map<String, String> categories = Map.of(
                "MERCHANT_001", "E_COMMERCE",
                "MERCHANT_002", "GAS_STATION",
                "MERCHANT_003", "WIRE_TRANSFER",
                "MERCHANT_004", "GROCERY",
                "MERCHANT_005", "ATM"
        );
        return categories.getOrDefault(merchantId, "UNKNOWN");
    }

    private String getRandomLocation() {
        String[] locations = {"New York, NY", "Los Angeles, CA", "Chicago, IL", "Houston, TX", "International"};
        return locations[random.nextInt(locations.length)];
    }

    private boolean isRuleTriggered(Map<String, Object> ruleResults, String ruleName) {
        Map<String, Object> rule = (Map<String, Object>) ruleResults.get(ruleName);
        return rule != null && (Boolean) rule.get("triggered");
    }

    private int countTriggeredRules(Map<String, Object> ruleResults) {
        return (int) ruleResults.values().stream()
                .filter(rule -> {
                    Map<String, Object> ruleMap = (Map<String, Object>) rule;
                    return (Boolean) ruleMap.get("triggered");
                })
                .count();
    }
}
