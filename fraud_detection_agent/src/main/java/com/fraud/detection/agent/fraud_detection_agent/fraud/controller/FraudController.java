package com.fraud.detection.agent.fraud_detection_agent.fraud.controller;


import com.fraud.detection.agent.fraud_detection_agent.fraud.model.Transaction;
import com.fraud.detection.agent.fraud_detection_agent.fraud.workflow.FraudDetectionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fraud")
public class FraudController {

    private final WorkflowServiceStubs serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();
    private final WorkflowClient client = WorkflowClient.newInstance(serviceStubs);

    @PostMapping("/check")
    public String checkFraud(@RequestBody Transaction transaction){

        FraudDetectionWorkflow workflow = client.newWorkflowStub(
                FraudDetectionWorkflow.class,
                WorkflowOptions.newBuilder().setTaskQueue("FRAUD_TASK_QUEUE")
                        .build()
        );


    System.out.println("transaction = " + transaction);
        workflow.detectFraud(transaction);
        return "Fraud check initiated for account:"+transaction.getAccountId();
    }
}
