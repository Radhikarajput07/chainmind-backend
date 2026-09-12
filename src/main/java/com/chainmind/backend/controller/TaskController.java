package com.chainmind.backend.controller;

import com.chainmind.backend.model.Task;
import com.chainmind.backend.model.Agent;
import com.chainmind.backend.service.DecisionService;
import com.chainmind.backend.service.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final List<Task> tasks = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Autowired
    private AgentController agentController;

    @Autowired
    private DecisionService decisionService;

    @Autowired
    private BlockchainService blockchainService;

    @PostMapping
    public Task createTask(@RequestBody Task taskRequest) {
        taskRequest.setId(idCounter.getAndIncrement());
        tasks.add(taskRequest);
        return taskRequest;
    }

    @GetMapping
    public List<Task> getAllTasks() {
        return tasks;
    }

    @PostMapping("/decide")
    public Map<String, Object> submitTaskForDecision(@RequestBody Task taskRequest) {
        taskRequest.setId(idCounter.getAndIncrement());
        tasks.add(taskRequest);

        List<Agent> availableAgents = agentController.getAllAgents();
        Map<String, Object> decisionResult = decisionService.decideAgent(taskRequest, availableAgents);

        Map<String, Object> response = new HashMap<>();
        response.put("task", taskRequest);
        response.put("availableAgents", availableAgents);
        response.put("decision", decisionResult.get("decision"));
        response.put("chosenAgent", decisionResult.get("chosenAgent"));
        response.put("reasoning", decisionResult.get("reasoning"));
        response.put("confidence", decisionResult.get("confidence"));
        response.put("aiReasoning", decisionResult.get("aiReasoning"));
        response.put("negotiationTranscript", decisionResult.get("negotiationTranscript"));
        response.put("workOutput", decisionResult.get("workOutput"));

        if ("SELECTED".equals(decisionResult.get("decision"))) {
            Agent chosen = (Agent) decisionResult.get("chosenAgent");
            try {
                BigInteger bountyAsWei = BigInteger.valueOf((long) taskRequest.getBounty());
                String txHash = blockchainService.createEscrow(chosen.getWalletAddress(), bountyAsWei);
                taskRequest.setEscrowTxHash(txHash);
                taskRequest.setStatus("CLAIMED");
                taskRequest.setClaimedByAgent(chosen.getName());
                response.put("escrowTxHash", txHash);
                response.put("explorerLink", blockchainService.getExplorerLink(txHash));
            } catch (Exception e) {
                response.put("blockchainError", e.getMessage());
            }
        }

        return response;
    }
}