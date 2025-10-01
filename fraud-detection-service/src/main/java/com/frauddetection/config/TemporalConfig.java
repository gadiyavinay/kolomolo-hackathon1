package com.frauddetection.config;

import com.frauddetection.activity.FraudDetectionActivitiesImpl;
import com.frauddetection.workflow.FraudDetectionWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(TemporalConfig.class);
    
    public static final String FRAUD_DETECTION_TASK_QUEUE = "fraud-detection-task-queue";
    
    @Value("${temporal.target:127.0.0.1:7233}")
    private String temporalServiceAddress;
    
    @Value("${temporal.namespace:default}")
    private String temporalNamespace;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        logger.info("Creating Temporal WorkflowServiceStubs for address: {}", temporalServiceAddress);
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalServiceAddress)
                        .build());
    }

    @Bean
    public WorkflowClient workflowClient(WorkflowServiceStubs workflowServiceStubs) {
        logger.info("Creating Temporal WorkflowClient for namespace: {}", temporalNamespace);
        return WorkflowClient.newInstance(workflowServiceStubs,
                WorkflowClientOptions.newBuilder()
                        .setNamespace(temporalNamespace)
                        .build());
    }

    @Bean
    public WorkerFactory workerFactory(WorkflowClient workflowClient) {
        logger.info("Creating Temporal WorkerFactory");
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    public CommandLineRunner temporalWorkerRunner(WorkerFactory workerFactory, 
                                                 FraudDetectionActivitiesImpl activities) {
        return args -> {
            logger.info("Starting Temporal Worker for task queue: {}", FRAUD_DETECTION_TASK_QUEUE);
            
            Worker worker = workerFactory.newWorker(FRAUD_DETECTION_TASK_QUEUE);
            
            // Register workflow implementation
            worker.registerWorkflowImplementationTypes(FraudDetectionWorkflowImpl.class);
            
            // Register activity implementation
            worker.registerActivitiesImplementations(activities);
            
            // Start the worker
            workerFactory.start();
            
            logger.info("Temporal Worker started successfully for fraud detection workflows");
        };
    }
}
