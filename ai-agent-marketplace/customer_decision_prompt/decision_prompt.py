import json
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
from shared.llm_client import call_llm

SYSTEM_PROMPT = """
You are a Customer Agent representing a client who needs a task completed.
Evaluate the available Worker Agents and choose the best one, then justify
your choice.

DECISION RULES:
1. Reject any agent whose price exceeds max_budget or delivery time exceeds deadline.
2. Among agents that pass, rank by weighted score:
   - price (lower better) - 40%
   - past_rating - 35%
   - delivery speed - 25%
3. If none pass, return decision "NO_SUITABLE_AGENT" with a reason.
4. If a good agent is close to budget, return decision "NEGOTIATE".

Respond ONLY in this exact JSON format, nothing else, no markdown code fences:
{
  "decision": "SELECTED" | "NO_SUITABLE_AGENT" | "NEGOTIATE",
  "chosen_agent_id": "<id or null>",
  "final_price_offered": <number or null>,
  "reasoning": "<short explanation>",
  "confidence": "<low/medium/high>"
}
"""


def _error_result(reason):
    return {
        "decision": "ERROR",
        "chosen_agent_id": None,
        "final_price_offered": None,
        "reasoning": reason,
        "confidence": "low"
    }


def _validate_agent(agent):
    if not isinstance(agent, dict):
        return False
    required = ["agent_id", "quoted_price", "estimated_delivery_time", "past_rating"]
    if not all(k in agent for k in required):
        return False
    return isinstance(agent["quoted_price"], (int, float)) and isinstance(agent["past_rating"], (int, float))


def build_user_message(task, agents):
    return f"""
TASK DETAILS:
{json.dumps(task, indent=2)}

AVAILABLE AGENTS:
{json.dumps(agents, indent=2)}
"""


def decide(task, agents):
    user_message = build_user_message(task, agents)

    try:
        raw_response = call_llm(SYSTEM_PROMPT, user_message)
    except Exception as e:
        return _error_result(f"LLM call failed: {str(e)}")

    if not raw_response or not isinstance(raw_response, str):
        return _error_result("LLM returned empty or invalid response")

    cleaned = raw_response.strip()
    if cleaned.startswith("```"):
        parts = cleaned.split("```")
        cleaned = parts[1] if len(parts) > 1 else cleaned
        if cleaned.startswith("json"):
            cleaned = cleaned[4:]
    cleaned = cleaned.strip()

    try:
        parsed = json.loads(cleaned)
    except json.JSONDecodeError:
        return _error_result(f"LLM did not return valid JSON: {raw_response}")

    if not isinstance(parsed, dict) or "decision" not in parsed:
        return _error_result(f"LLM JSON missing required fields: {raw_response}")

    return parsed


def select_best_agent(task_description, max_budget, deadline, quality_level, available_agents):
    if not task_description or not isinstance(task_description, str):
        return _error_result("task_description is missing or invalid")

    if not isinstance(max_budget, (int, float)) or max_budget <= 0:
        return _error_result("max_budget is missing or invalid")

    if not deadline or not isinstance(deadline, str):
        return _error_result("deadline is missing or invalid")

    if not quality_level or not isinstance(quality_level, str):
        return _error_result("quality_level is missing or invalid")

    if not isinstance(available_agents, list):
        return _error_result("available_agents must be a list")

    if len(available_agents) == 0:
        return {
            "decision": "NO_SUITABLE_AGENT",
            "chosen_agent_id": None,
            "final_price_offered": None,
            "reasoning": "No agents available to evaluate",
            "confidence": "high"
        }

    valid_agents = [a for a in available_agents if _validate_agent(a)]

    if len(valid_agents) == 0:
        return _error_result("No valid agent objects found in available_agents")

    task = {
        "task_description": task_description,
        "max_budget": max_budget,
        "deadline": deadline,
        "quality_level": quality_level
    }

    return decide(task, valid_agents)


def print_decision_result(result):
    print("-" * 50)
    print(f"Decision      : {result.get('decision')}")
    print(f"Chosen Agent  : {result.get('chosen_agent_id')}")
    print(f"Final Price   : {result.get('final_price_offered')}")
    print(f"Confidence    : {result.get('confidence')}")
    print(f"Reasoning     : {result.get('reasoning')}")
    print("-" * 50)