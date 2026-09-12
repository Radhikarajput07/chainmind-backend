package com.chainmind.backend.service;

import com.chainmind.backend.model.Agent;
import com.chainmind.backend.model.Task;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DecisionService {

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
        result.put("reasoning", "Temporary logic: best price-to-rating balance ke basis pe select kiya gaya.");
        result.put("confidence", "medium");

        return result;
    }
}
