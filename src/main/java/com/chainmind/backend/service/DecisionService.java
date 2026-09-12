package com.chainmind.backend.service;

import com.chainmind.backend.model.Agent;
import com.chainmind.backend.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DecisionService {

    @Autowired
    private LlmService llmService;

    public Map<String, Object> decideAgent(Task task, List<Agent> availableAgents) {

        Map<String, Object> result = new HashMap<>();

        List<Agent> affordableAgents = new ArrayList<>();
        for (Agent agent : availableAgents) {
            if (agent.getPrice() <= task.getBounty()) {
                affordableAgents.add(agent);
            }
        }

        if (affordableAgents.isEmpty()) {
            result.put("decision", "NO_SUITABLE_AGENT");
            result.put("chosenAgent", null);
            result.put("reasoning", "Koi bhi agent task ke budget (" + task.getBounty() + ") ke andar available nahi hai.");
            result.put("confidence", "high");
            return result;
        }

        Agent bestAgent = null;
        double bestScore = -1;

        for (Agent agent : affordableAgents) {
            double priceScore = 1 - (agent.getPrice() / task.getBounty());
            double ratingScore = agent.getPastRating() / 5.0;
            double totalScore = (priceScore * 0.5) + (ratingScore * 0.5);
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestAgent = agent;
            }
        }

        result.put("decision", "SELECTED");
        result.put("chosenAgent", bestAgent);
        result.put("reasoning", "Best price-to-rating balance ke basis pe select kiya gaya.");
        result.put("confidence", "medium");

        if (bestAgent != null) {
            // ---- 1. Negotiation dialogue (real price haggling, AI generated) ----
            String negotiationPrompt = "Task: " + task.getTitle() + " (client's budget: $" + task.getBounty() + "). "
                + bestAgent.getName() + " normally charges $" + bestAgent.getPrice() + ". "
                + "Write a realistic short price negotiation between 'Client' and '" + bestAgent.getName() + "'. "
                + "The client should push back on price at least once with a specific lower dollar number, "
                + "the agent should counter with a number in between, and they should agree on a final number. "
                + "Write exactly 5 lines, alternating speakers, plain text, no markdown, format strictly as:\n"
                + "Client: <message with a $ amount>\n"
                + bestAgent.getName() + ": <message with a $ amount>\n"
                + "Client: <message>\n"
                + bestAgent.getName() + ": <message with final $ amount>\n"
                + "Client: <short message agreeing>\n";
            String negotiationTranscript = llmService.askAi(negotiationPrompt);
            result.put("negotiationTranscript", negotiationTranscript);

            // ---- 2. Why this agent ----
            String reasonPrompt = "Task: " + task.getTitle() + " (budget: " + task.getBounty() + "). "
                + "Agent chosen: " + bestAgent.getName() + " with price " + bestAgent.getPrice()
                + " and rating " + bestAgent.getPastRating() + ". "
                + "In one short sentence, explain why this is a good match.";
            result.put("aiReasoning", llmService.askAi(reasonPrompt));

            // ---- 3. Actual work output (agent "delivers" the task) ----
            String workPrompt = "You are " + bestAgent.getName() + ", an AI agent specializing in " + bestAgent.getTaskType() + ". "
                + "You just agreed to do this task: \"" + task.getTitle() + " - " + task.getDescription() + "\". "
                + "Write your actual deliverable now, as if handing over finished work. "
                + "Keep it to 3-4 concise sentences or short bullet points. Do not add any preamble like 'Sure, here is...' - just the work itself.";
            result.put("workOutput", llmService.askAi(workPrompt));
        }

        return result;
    }
}