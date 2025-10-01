package com.frauddetection.controller;

import com.frauddetection.config.TemporalConfig;
import com.frauddetection.model.FraudDetectionResult;
import com.frauddetection.model.FraudStatus;
import com.frauddetection.model.TransactionData;
import com.frauddetection.workflow.FraudDetectionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/fraud-detection")
@CrossOrigin(origins = "*")
public class FraudDetectionController {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionController.class);
    
    @Autowired
    private WorkflowClient workflowClient;

    @PostMapping("/startTransaction")
    public ResponseEntity<Map<String, Object>> startTransaction(@Valid @RequestBody TransactionData transactionData) {
        logger.info("Received fraud detection request for transaction: {}", transactionData.getTransactionId());
        
        try {
            // Generate workflow ID
            String workflowId = "fraud-detection-" + UUID.randomUUID().toString();
            
            // Configure workflow options
            WorkflowOptions options = WorkflowOptions.newBuilder()
                    .setWorkflowId(workflowId)
                    .setTaskQueue(TemporalConfig.FRAUD_DETECTION_TASK_QUEUE)
                    .setWorkflowExecutionTimeout(Duration.ofMinutes(5))
                    .build();
            
            // Create workflow stub
            FraudDetectionWorkflow workflow = workflowClient.newWorkflowStub(FraudDetectionWorkflow.class, options);
            
            // Start workflow asynchronously
            CompletableFuture<FraudDetectionResult> future = WorkflowClient.execute(workflow::processFraudDetection, transactionData);
            
            // Return immediate response
            Map<String, Object> response = Map.of(
                    "transactionId", transactionData.getTransactionId(),
                    "workflowId", workflowId,
                    "status", FraudStatus.PENDING.name(),
                    "message", "Fraud detection workflow started",
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            logger.info("Fraud detection workflow started for transaction: {} with workflowId: {}", 
                       transactionData.getTransactionId(), workflowId);
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            logger.error("Error starting fraud detection workflow for transaction: {}", 
                        transactionData.getTransactionId(), e);
            
            Map<String, Object> errorResponse = Map.of(
                    "transactionId", transactionData.getTransactionId(),
                    "status", FraudStatus.ERROR.name(),
                    "message", "Error starting fraud detection: " + e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/status/{workflowId}")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(@PathVariable String workflowId) {
        logger.info("Checking status for workflow: {}", workflowId);
        
        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
            
            // Check if workflow is still running
            boolean isRunning = true;
            try {
                workflowStub.getResult(1, TimeUnit.MILLISECONDS, FraudDetectionResult.class);
                isRunning = false; // If we get here, workflow is complete
            } catch (Exception e) {
                // Workflow still running or failed
                isRunning = true;
            }
            
            Map<String, Object> response = Map.of(
                    "workflowId", workflowId,
                    "isRunning", isRunning,
                    "status", isRunning ? FraudStatus.IN_PROGRESS.name() : FraudStatus.COMPLETED.name(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking workflow status: {}", workflowId, e);
            
            Map<String, Object> errorResponse = Map.of(
                    "workflowId", workflowId,
                    "status", FraudStatus.ERROR.name(),
                    "message", "Error checking workflow status: " + e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/result/{workflowId}")
    public ResponseEntity<FraudDetectionResult> getWorkflowResult(@PathVariable String workflowId) {
        logger.info("Getting result for workflow: {}", workflowId);
        
        try {
            WorkflowStub workflowStub = workflowClient.newUntypedWorkflowStub(workflowId);
            FraudDetectionResult result = workflowStub.getResult(1, TimeUnit.SECONDS, FraudDetectionResult.class);
            
            logger.info("Retrieved fraud detection result for workflow: {} with decision: {}", 
                       workflowId, result.getDecision());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Error getting workflow result: {}", workflowId, e);
            
            FraudDetectionResult errorResult = new FraudDetectionResult();
            errorResult.setWorkflowId(workflowId);
            errorResult.setStatus(FraudStatus.ERROR);
            errorResult.setReason("Error retrieving workflow result: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult);
        }
    }

    @PostMapping("/demo/sample-transaction")
    public ResponseEntity<Map<String, Object>> createSampleTransaction(@RequestParam(defaultValue = "safe") String riskProfile) {
        logger.info("Creating sample transaction with risk profile: {}", riskProfile);
        
        TransactionData sampleTransaction = createSampleTransactionData(riskProfile);
        
        Map<String, Object> response = Map.of(
                "message", "Sample transaction created",
                "riskProfile", riskProfile,
                "transactionData", sampleTransaction,
                "timestamp", java.time.LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/demo/process-sample")
    public ResponseEntity<Map<String, Object>> processSampleTransaction(@RequestParam(defaultValue = "safe") String riskProfile) {
        logger.info("Processing sample transaction with risk profile: {}", riskProfile);
        
        TransactionData sampleTransaction = createSampleTransactionData(riskProfile);
        return startTransaction(sampleTransaction);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "service", "fraud-detection-service",
                "status", "healthy",
                "timestamp", java.time.LocalDateTime.now(),
                "version", "1.0.0"
        );
        
        return ResponseEntity.ok(health);
    }

    private TransactionData createSampleTransactionData(String riskProfile) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerId = "CUST-" + (1000 + (int)(Math.random() * 9000));
        String accountId = "ACC-" + (10000 + (int)(Math.random() * 90000));
        
        TransactionData transaction = new TransactionData();
        transaction.setTransactionId(transactionId);
        transaction.setCustomerId(customerId);
        transaction.setAccountId(accountId);
        transaction.setCurrency("USD");
        
        // Configure based on risk profile
        switch (riskProfile.toLowerCase()) {
            case "risky":
                transaction.setAmount(new BigDecimal("5000.00")); // High amount
                transaction.setMerchantId("MERCHANT_003"); // Wire transfer (risky)
                transaction.setLocation("International");
                transaction.setCountry("Unknown");
                transaction.setChannel("ONLINE");
                break;
                
            case "fraudulent":
                transaction.setAmount(new BigDecimal("10000.00")); // Very high amount
                transaction.setMerchantId("MERCHANT_003"); // Wire transfer
                transaction.setLocation("International");
                transaction.setCountry("Suspicious");
                transaction.setChannel("ATM");
                break;
                
            case "safe":
            default:
                transaction.setAmount(new BigDecimal("150.00")); // Normal amount
                transaction.setMerchantId("MERCHANT_001"); // Amazon (safe)
                transaction.setLocation("New York, NY");
                transaction.setCountry("USA");
                transaction.setChannel("ONLINE");
                break;
        }
        
        return transaction;
    }
}
