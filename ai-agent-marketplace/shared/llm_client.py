import os
from typing import Optional

import google.generativeai as genai
from dotenv import load_dotenv

load_dotenv()

genai.configure(api_key=os.environ.get("GOOGLE_API_KEY"))

def call_llm(system_prompt: str, user_message: str, model="gemini-2.5-flash") -> str:
    """
    Common function jo koi bhi agent (Client, Worker, Customer) use karega
    Gemini ko call karne ke liye.
    """
    gemini_model = genai.GenerativeModel(
        model_name=model,
        system_instruction=system_prompt
    )
    response = gemini_model.generate_content(user_message)
    return response.text


class text:
    """A small convenience wrapper around the Gemini client for common text tasks."""

    def __init__(self, model: str = "gemini-2.5-flash") -> None:
        self.model = model

    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        model: Optional[str] = None,
    ) -> str:
        """Generate text from a user prompt using the configured Gemini model."""
        final_system_prompt = system_prompt or "You are a helpful and concise assistant."
        final_model = model or self.model
        return call_llm(final_system_prompt, prompt, final_model)

    def summarize(
        self,
        text_to_summarize: str,
        max_length: int = 200,
        model: Optional[str] = None,
    ) -> str:
        """Create a concise summary from the provided text."""
        system_prompt = (
            "You are an expert summarizer. Produce a clear, concise summary "
            f"in at most {max_length} characters."
        )
        return self.generate(text_to_summarize, system_prompt=system_prompt, model=model)

    def chat(
        self,
        user_message: str,
        system_prompt: str = "You are a helpful assistant.",
        model: Optional[str] = None,
    ) -> str:
        """Send a chat-style request to Gemini and return the response text."""
        return self.generate(user_message, system_prompt=system_prompt, model=model)


Text = text