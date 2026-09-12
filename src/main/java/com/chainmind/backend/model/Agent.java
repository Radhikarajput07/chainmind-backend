package com.chainmind.backend.model;

public class Agent {
    private String id;
    private String name;
    private String taskType;
    private double price;
    private double pastRating;
    private String estimatedDeliveryTime;
    private String status;
    private String walletAddress;

    public Agent() {}

    public Agent(String id, String name, String taskType, double price, 
                 double pastRating, String estimatedDeliveryTime) {
        this.id = id;
        this.name = name;
        this.taskType = taskType;
        this.price = price;
        this.pastRating = pastRating;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.status = "AVAILABLE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public double getPastRating() { return pastRating; }
    public void setPastRating(double pastRating) { this.pastRating = pastRating; }
    public String getEstimatedDeliveryTime() { return estimatedDeliveryTime; }
    public void setEstimatedDeliveryTime(String estimatedDeliveryTime) { this.estimatedDeliveryTime = estimatedDeliveryTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }
}