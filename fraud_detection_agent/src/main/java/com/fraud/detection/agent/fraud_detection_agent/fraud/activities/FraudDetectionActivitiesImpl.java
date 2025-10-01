package com.fraud.detection.agent.fraud_detection_agent.fraud.activities;

import com.fraud.detection.agent.fraud_detection_agent.fraud.model.Transaction;

public class FraudDetectionActivitiesImpl implements FraudDetectionActivities{

  @Override
  public double scoreTransaction(Transaction transaction){
      // Simulated ML scoring logic
      return (transaction.getAmount() > 1000 || "Unknown".equalsIgnoreCase(transaction.getLocation())) ? 0.9 : 0.3;
  }

    @Override
    public void freezeAccount(String accountId) {
        System.out.println("Freezing account :"+accountId);
    }

    @Override
    public void notifyUser(String accountId) {
        System.out.println("Notifying user of suspicious activity on account :"+accountId);
    }

    @Override
    public void notifyDownstream(Transaction transaction) {
        System.out.println("Notifying downstream systems for account :"+transaction.getAccountId());
    }

    @Override
    public void logAudit(Transaction transaction, double score){
      System.out.printf("Audit Log - Account: %s, Amount %.2f, Location: %s, Score: %2f%n", transaction.getAccountId(), transaction.getAmount(), transaction.getLocation(),score);
    }

}
