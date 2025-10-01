package com.fraud.detection.agent.fraud_detection_agent.fraud.activities;

import com.fraud.detection.agent.fraud_detection_agent.fraud.workflow.FraudDetectionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class FraudWorker {

    public static void main(String[] args){

        // Connect to Temporal service
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create Worker Factory
        WorkerFactory factory = WorkerFactory.newInstance(client);

        //Create Worker and register workflow + activities
        Worker worker = factory.newWorker("FRAUD_TASK_QUEUE");

        worker.registerWorkflowImplementationTypes(FraudDetectionWorkflowImpl.class);
        worker.registerActivitiesImplementations(new FraudDetectionActivitiesImpl());

        // Start polling
        factory.start();
    }
}
