package com.fraud.detection.agent.fraud_detection_agent.fraud.workflow;

import com.fraud.detection.agent.fraud_detection_agent.fraud.model.Transaction;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FraudDetectionWorkflow {

    @WorkflowMethod
    void detectFraud(Transaction transaction);

    @SignalMethod
    void approveTransaction();
}
