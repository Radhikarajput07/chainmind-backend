# Customer Decision Agent

## Purpose
Yeh module client ke task requirements aur available worker agents ki list
dekhkar best agent select karta hai — price, rating, aur delivery time ke
weighted score ke basis pe.

## Files
- `decision_prompt.py` — Main logic: system prompt + decision function
- `test_decision.py` — Sample test case

## How it works
1. Task details (budget, deadline, quality) aur agents ki list input di jaati hai
2. LLM (Gemini) ko structured prompt diya jaata hai
3. LLM strict JSON format me decision return karta hai:
   - `SELECTED` — best agent chosen
   - `NO_SUITABLE_AGENT` — koi agent criteria match nahi karta
   - `NEGOTIATE` — price thoda adjust ho sakta hai

## Decision Logic (Weights)
- Price: 40%
- Past rating: 35%
- Delivery speed: 25%

## How to run
\`\`\`bash
cd customer_decision_prompt
python decision_prompt.py
\`\`\`

## Author
PRINCE