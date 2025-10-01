package com.fraud.detection.agent.fraud_detection_agent.fraud.model;

public class Transaction {

    private String accountId;
    private double amount;
    private String location;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "accountId='" + accountId + '\'' +
                ", amount=" + amount +
                ", location='" + location + '\'' +
                '}';
    }
}
