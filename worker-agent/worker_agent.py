import os
import sys
import json
import time
import datetime
import requests
from openai import OpenAI
from dotenv import load_dotenv
from flask import Flask, request, jsonify

load_dotenv()

client = OpenAI(
    api_key=os.getenv("OPENROUTER_API_KEY"),
    base_url="https://openrouter.ai/api/v1"
)

REGISTRY_BASE_URL = "http://192.168.7.206:8080"
WALLET_ADDRESS = "0xREPLACE_WITH_REAL_WALLET_ADDRESS"

MODELS = [
    "openrouter/free",
    "deepseek/deepseek-v4-flash:free",
    "moonshotai/kimi-k2.6:free"
]
PRICE = 0.01

app = Flask(__name__)

CYAN = "\033[96m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
MAGENTA = "\033[95m"
RED = "\033[91m"
BOLD = "\033[1m"
DIM = "\033[2m"
RESET = "\033[0m"

WIDTH = 52
MAX_INPUT_CHARS = 6000


def call_model(prompt, timeout_seconds=20):
    raw_text = None
    last_error = None
    used_model = None

    for model in MODELS:
        try:
            response = client.chat.completions.create(
                model=model,
                messages=[{"role": "user", "content": prompt}],
                timeout=timeout_seconds
            )
            if not response.choices or not response.choices[0].message.content:
                last_error = ValueError(f"Empty response from model {model}")
                continue
            raw_text = response.choices[0].message.content.strip()
            used_model = model
            break
        except Exception as e:
            last_error = e
            continue

    if raw_text is None:
        raise RuntimeError(f"All models failed. Last error: {last_error}")

    if raw_text.startswith("```"):
        raw_text = raw_text.strip("`")
        raw_text = raw_text.replace("json", "", 1).strip()

    return raw_text, used_model


def summarizer_agent(text):
    prompt = f"""Summarize the following text in 2-3 punchy, engaging sentences.
Also give it a short catchy title (max 6 words), and a one-word "vibe" tag
describing the tone (e.g. Optimistic, Technical, Urgent, Chill, Serious).

Text:
{text}

Return ONLY valid JSON, no extra text, in this exact format:
{{
    "title": "",
    "summary": "",
    "vibe": ""
}}
"""

    raw_text, used_model = call_model(prompt)

    try:
        parsed = json.loads(raw_text)
    except json.JSONDecodeError:
        parsed = {"title": "Summary", "summary": raw_text, "vibe": "Neutral"}

    return {
        "agent": "Summarizer",
        "status": "success",
        "price": PRICE,
        "currency": "Faucets",
        "title": parsed.get("title", ""),
        "summary": parsed.get("summary", ""),
        "vibe": parsed.get("vibe", "Neutral"),
        "model_used": used_model,
        "input_word_count": len(text.split()),
        "output_word_count": len(parsed.get("summary", "").split()),
        "timestamp": datetime.datetime.now().strftime("%H:%M:%S")
    }


def sentiment_agent(text):
    prompt = f"""Analyze the sentiment of the following text.

Text:
{text}

Return ONLY valid JSON, no extra text, in this exact format:
{{
    "sentiment": "Positive/Negative/Neutral",
    "confidence": 0.0,
    "reason": "one short sentence explaining why"
}}
"""

    raw_text, used_model = call_model(prompt)

    try:
        parsed = json.loads(raw_text)
    except json.JSONDecodeError:
        parsed = {"sentiment": "Neutral", "confidence": 0.5, "reason": raw_text}

    return {
        "agent": "Sentiment",
        "status": "success",
        "price": PRICE,
        "currency": "Faucets",
        "sentiment": parsed.get("sentiment", "Neutral"),
        "confidence": parsed.get("confidence", 0.5),
        "reason": parsed.get("reason", ""),
        "model_used": used_model,
        "input_word_count": len(text.split()),
        "timestamp": datetime.datetime.now().strftime("%H:%M:%S")
    }


def translator_agent(text, target_lang):
    prompt = f"""Translate the following text into {target_lang}.

Text:
{text}

Return ONLY valid JSON, no extra text, in this exact format:
{{
    "translation": ""
}}
"""

    raw_text, used_model = call_model(prompt)

    try:
        parsed = json.loads(raw_text)
    except json.JSONDecodeError:
        parsed = {"translation": raw_text}

    return {
        "agent": "Translator",
        "status": "success",
        "price": PRICE,
        "currency": "Faucets",
        "target_lang": target_lang,
        "translation": parsed.get("translation", ""),
        "model_used": used_model,
        "input_word_count": len(text.split()),
        "timestamp": datetime.datetime.now().strftime("%H:%M:%S")
    }


AGENTS_TO_REGISTER = [
    {
        "name": "Summarizer-Agent",
        "taskType": "summarization",
        "price": PRICE,
        "pastRating": 4.5,
        "estimatedDeliveryTime": "10 seconds",
        "walletAddress": WALLET_ADDRESS
    },
    {
        "name": "Sentiment-Agent",
        "taskType": "sentiment-analysis",
        "price": PRICE,
        "pastRating": 4.5,
        "estimatedDeliveryTime": "10 seconds",
        "walletAddress": WALLET_ADDRESS
    },
    {
        "name": "Translator-Agent",
        "taskType": "translation",
        "price": PRICE,
        "pastRating": 4.5,
        "estimatedDeliveryTime": "10 seconds",
        "walletAddress": WALLET_ADDRESS
    }
]


def register_agents():
    for agent in AGENTS_TO_REGISTER:
        try:
            response = requests.post(
                f"{REGISTRY_BASE_URL}/api/agents",
                json=agent,
                timeout=10
            )
            if response.status_code in (200, 201):
                print(f"[Registry] Registered: {agent['name']}")
            else:
                print(f"[Registry] Failed to register {agent['name']}: "
                      f"{response.status_code} - {response.text}")
        except Exception as e:
            print(f"[Registry] Error registering {agent['name']}: {e}")


def get_valid_text(data):
    if data is None:
        return None, (jsonify({"error": "Invalid or missing JSON body"}), 400)

    text = data.get("text", "")

    if not isinstance(text, str):
        return None, (jsonify({"error": "text must be a string"}), 400)

    text = text.strip()

    if not text:
        return None, (jsonify({"error": "text is required and cannot be empty"}), 400)

    if len(text) > MAX_INPUT_CHARS:
        return None, (jsonify({
            "error": f"text is too long ({len(text)} chars). Max allowed is {MAX_INPUT_CHARS}."
        }), 400)

    return text, None


@app.route("/worker/summarize", methods=["POST"])
def summarize_endpoint():
    data = request.get_json(silent=True)
    text, error_response = get_valid_text(data)
    if error_response:
        return error_response

    try:
        result = summarizer_agent(text)
    except Exception as e:
        return jsonify({
            "agent": "Summarizer",
            "status": "error",
            "message": str(e),
            "timestamp": datetime.datetime.now().strftime("%H:%M:%S")
        }), 500

    return jsonify(result)


@app.route("/worker/sentiment", methods=["POST"])
def sentiment_endpoint():
    data = request.get_json(silent=True)
    text, error_response = get_valid_text(data)
    if error_response:
        return error_response

    try:
        result = sentiment_agent(text)
    except Exception as e:
        return jsonify({
            "agent": "Sentiment",
            "status": "error",
            "message": str(e),
            "timestamp": datetime.datetime.now().strftime("%H:%M:%S")
        }), 500

    return jsonify(result)


@app.route("/worker/translate", methods=["POST"])
def translate_endpoint():
    data = request.get_json(silent=True)
    text, error_response = get_valid_text(data)
    if error_response:
        return error_response

    target_lang = data.get("target_lang", "Hindi")
    if not isinstance(target_lang, str) or not target_lang.strip():
        target_lang = "Hindi"

    try:
        result = translator_agent(text, target_lang)
    except Exception as e:
        return jsonify({
            "agent": "Translator",
            "status": "error",
            "message": str(e),
            "timestamp": datetime.datetime.now().strftime("%H:%M:%S")
        }), 500

    return jsonify(result)


if __name__ == "__main__":
    if "--server" in sys.argv:
        register_agents()
        app.run(debug=True, port=5000)
    else:
        demo_text = (
            "Artificial intelligence is transforming how students learn. "
            "It helps automate repetitive tasks, personalize learning paths, "
            "and gives instant feedback. However, it should support human "
            "teachers, not replace them."
        )
        result = summarizer_agent(demo_text)
        print(json.dumps(result, indent=2))

        result2 = sentiment_agent(demo_text)
        print(json.dumps(result2, indent=2))

        result3 = translator_agent(demo_text, "Hindi")
        print(json.dumps(result3, indent=2))