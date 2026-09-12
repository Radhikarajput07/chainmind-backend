package com.chainmind.backend.controller;

import com.chainmind.backend.model.Agent;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    private final List<Agent> agents = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    public AgentController() {
        Agent alpha = new Agent(String.valueOf(idCounter.getAndIncrement()), "Agent-Alpha", "negotiation", 10.0, 4.5, "2 hours");
        alpha.setWalletAddress("0x000000000000000000000000000000000000dEaD");
        agents.add(alpha);

        Agent beta = new Agent(String.valueOf(idCounter.getAndIncrement()), "Agent-Beta", "writing", 18.0, 4.8, "1 hour");
        beta.setWalletAddress("0x000000000000000000000000000000000000dEaD");
        agents.add(beta);

        Agent gamma = new Agent(String.valueOf(idCounter.getAndIncrement()), "Agent-Gamma", "research", 6.0, 3.9, "4 hours");
        gamma.setWalletAddress("0x000000000000000000000000000000000000dEaD");
        agents.add(gamma);
    }

    @PostMapping
    public Agent registerAgent(@RequestBody Agent agentRequest) {
        Agent agent = new Agent(
            String.valueOf(idCounter.getAndIncrement()),
            agentRequest.getName(),
            agentRequest.getTaskType(),
            agentRequest.getPrice(),
            agentRequest.getPastRating(),
            agentRequest.getEstimatedDeliveryTime()
        );
        agent.setWalletAddress(agentRequest.getWalletAddress());
        agents.add(agent);
        return agent;
    }

    @GetMapping
    public List<Agent> getAllAgents() {
        return agents;
    }

    @GetMapping("/{id}")
    public Agent getAgentById(@PathVariable String id) {
        return agents.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
    }

    @PutMapping("/{id}/status")
    public Agent updateStatus(@PathVariable String id, @RequestParam String status) {
        Agent agent = getAgentById(id);
        if (agent != null) agent.setStatus(status);
        return agent;
    }
}