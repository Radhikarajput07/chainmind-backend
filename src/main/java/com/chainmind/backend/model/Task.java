package com.chainmind.backend.model;

public class Task {
    private Long id;
    private String title;
    private String description;
    private double bounty;
    private String status; // "OPEN", "CLAIMED", "COMPLETED"
    private String claimedByAgent;

    public Task() {}

    public Task(Long id, String title, String description, double bounty) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.bounty = bounty;
        this.status = "OPEN";
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getBounty() { return bounty; }
    public void setBounty(double bounty) { this.bounty = bounty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getClaimedByAgent() { return claimedByAgent; }
    public void setClaimedByAgent(String claimedByAgent) { this.claimedByAgent = claimedByAgent; }
}