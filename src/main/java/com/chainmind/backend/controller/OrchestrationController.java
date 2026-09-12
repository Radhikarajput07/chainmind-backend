package com.chainmind.backend.controller;

import com.chainmind.backend.model.Agent;
import com.chainmind.backend.model.Task;
import com.chainmind.backend.service.DecisionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orchestration")
@CrossOrigin(origins = "*")
public class OrchestrationController {

    private final TaskController taskController;
    private final AgentController agentController;
    private final DecisionService decisionService;

    public OrchestrationController(
            TaskController taskController,
            AgentController agentController,
            DecisionService decisionService) {

        this.taskController = taskController;
        this.agentController = agentController;
        this.decisionService = decisionService;
    }

    @PostMapping("/assign/{taskId}")
    public Map<String, Object> assignAgentToTask(
            @PathVariable Long taskId) {

        Task task = taskController.getTaskById(taskId);

        if (task == null) {
            return Map.of("error", "Task not found");
        }

        List<Agent> availableAgents =
                agentController.getAllAgents()
                        .stream()
                        .filter(a -> "AVAILABLE".equals(a.getStatus()))
                        .collect(Collectors.toList());

        Map<String, Object> decision =
                decisionService.decideAgent(task, availableAgents);

        if ("SELECTED".equals(decision.get("decision"))) {

            Agent chosen = (Agent) decision.get("chosenAgent");

            taskController.claimTask(
                    taskId,
                    chosen.getName()
            );

            agentController.updateStatus(
                    chosen.getId(),
                    "BUSY"
            );
        }

        return decision;
    }
}