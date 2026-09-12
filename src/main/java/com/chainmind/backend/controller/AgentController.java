package com.chainmind.backend.controller;

import com.chainmind.backend.model.Agent;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*")
public class AgentController {

    private final List<Agent> agents = new ArrayList<>();

    public AgentController() {
        agents.add(new Agent("worker_1", "Summarizer", "summarize", 50, 4.2, "1 hour"));
        agents.add(new Agent("worker_2", "SentimentAnalyzer", "sentiment", 30, 4.5, "30 mins"));
        agents.add(new Agent("worker_3", "Translator", "translate", 45, 4.0, "2 hours"));
    }

    @GetMapping
    public List<Agent> getAllAgents() {
        return agents;
    }

    @GetMapping("/{id}")
    public Agent getAgentById(@PathVariable String id) {
        return agents.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
}
