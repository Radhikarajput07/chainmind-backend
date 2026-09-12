from decision_prompt import select_best_agent, print_decision_result

print("\n=== TEST: Deadline exceeded (should reject) ===")
agents = [
    {"agent_id": "worker_10", "quoted_price": 30, "estimated_delivery_time": "5 hours", "past_rating": 4.7},
]
result = select_best_agent(
    task_description="Simple task",
    max_budget=50,
    deadline="1 hour",
    quality_level="low",
    available_agents=agents
)
print_decision_result(result)

print("\n=== TEST: Cheap but low rating vs expensive but high rating ===")
agents2 = [
    {"agent_id": "worker_11", "quoted_price": 20, "estimated_delivery_time": "1 hour", "past_rating": 2.5},
    {"agent_id": "worker_12", "quoted_price": 55, "estimated_delivery_time": "1 hour", "past_rating": 4.9},
]
result2 = select_best_agent(
    task_description="Poster design",
    max_budget=60,
    deadline="2 hours",
    quality_level="high",
    available_agents=agents2
)
print_decision_result(result2)