package com.frauddetection.workflow;

import com.frauddetection.model.FraudDetectionResult;
import com.frauddetection.model.TransactionData;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FraudDetectionWorkflow {
    
    @WorkflowMethod
    FraudDetectionResult processFraudDetection(TransactionData transactionData);
}
