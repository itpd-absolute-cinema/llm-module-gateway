"""Local demonstration provider. Returns fixed synthetic text, without LLM calls."""

import asyncio
import os
from time import time
from uuid import uuid4

from fastapi import FastAPI
from fastapi.responses import JSONResponse

app = FastAPI(title="Demo upstream (local use only)")


@app.post("/v1/chat/completions")
async def completion(body: dict):
    await asyncio.sleep(float(os.environ.get("DEMO_DELAY_SECONDS", "0")))
    status = int(os.environ.get("DEMO_STATUS_CODE", "200"))
    if status != 200:
        return JSONResponse(
            {"error": {"message": "Demo upstream failure", "type": "server_error", "code": "demo_error"}},
            status_code=status,
        )
    return {
        "id": "chatcmpl-" + uuid4().hex,
        "object": "chat.completion",
        "created": int(time()),
        "model": body["model"],
        "choices": [{
            "index": 0,
            "message": {"role": "assistant", "content": "Demo response containing SECRET."},
            "finish_reason": "stop",
        }],
        "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
    }
