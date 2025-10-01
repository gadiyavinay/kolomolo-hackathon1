package com.fraud.detection.agent.fraud_detection_agent.fraud.workflow;

import com.fraud.detection.agent.fraud_detection_agent.fraud.activities.FraudDetectionActivities;
import com.fraud.detection.agent.fraud_detection_agent.fraud.model.Transaction;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class FraudDetectionWorkflowImpl implements FraudDetectionWorkflow{

    private final FraudDetectionActivities activities = Workflow.newActivityStub(FraudDetectionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(2))
                            .build())
                    .build());

    private boolean approved = false;

    @Override
    public void detectFraud(Transaction transaction){

    System.out.println("transaction = " + transaction);

        // Step 1 : ML scoring
        double score = activities.scoreTransaction(transaction);

        // Step 2 : Log audit trail
        activities.logAudit(transaction, score);

        // Step 3: Wait for human approval if score is suspicious
        if(score > 0.8){
            Workflow.await(() -> approved); // Wait for signal
        }

        // Step 4: Escalate if approved or score is high
        if(score > 0.8 || approved){
            activities.freezeAccount(transaction.getAccountId());
            activities.notifyUser(transaction.getAccountId());
            activities.notifyDownstream(transaction);
        }
    }

    @Override
    public void approveTransaction(){
        approved = true;
    }

}
