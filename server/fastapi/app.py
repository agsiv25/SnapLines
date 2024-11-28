from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.responses import JSONResponse, Response
import httpx
from pathlib import Path
import shutil
import base64
import os
import json
from openai import OpenAI

app = FastAPI()

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

SHARED_DIR = Path("/shared")
SHARED_DIR.mkdir(parents=True, exist_ok=True)

def encode_image(image_path: str) -> str:
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode("utf-8")

@app.get('/')
def home():
    return {"hello": "world"}

@app.get('/prompt')
async def ask(prompt: str):
    if not prompt:
        raise HTTPException(status_code=400, detail="Prompt is required")
    
    try:
        async with httpx.AsyncClient() as client:
            res = await client.post('http://ollama:11434/api/generate', json={
                "prompt": prompt,
                "stream": False,
                "model": "bakllava"
            })
            res.raise_for_status()
    except httpx.RequestError as e:
        return JSONResponse(content={"error": str(e)}, status_code=500)
    
    return Response(content=res.text, media_type="application/json")

# @app.post('/football')
# async def football(file: UploadFile = File(...)):

#     if not file.filename.lower().endswith(('.png', '.jpg', '.jpeg')):
#         raise HTTPException(status_code=400, detail="Invalid file type. Only PNG, JPG, and JPEG are supported.")
    
#     file_path = SHARED_DIR / file.filename
#     with file_path.open("wb") as buffer:
#         shutil.copyfileobj(file.file, buffer)

#     prompt = f"Which two teams are playing in this image: \"/shared/{file.filename}\""

#     try:
#         async with httpx.AsyncClient(timeout=600) as client:
#             res = await client.post('http://ollama:11434/api/generate', json={
#                 "prompt": prompt,
#                 "stream": False,
#                 "model": "bakllava"
#             })
#             res.raise_for_status()
#     except httpx.RequestError as e:
#         return JSONResponse(content={"error": str(e)}, status_code=500)
    

#     file_path.unlink(missing_ok=True)

#     return Response(content=res.text, media_type="application/json")

@app.post('/test')
async def test_prompt(
    file: UploadFile = File(...),
):
    
    prompt = "what are the two nfl teams in this image, give the answer as (team1, team2)"
    
    if not file.filename.lower().endswith(('.png', '.jpg', '.jpeg')):
        raise HTTPException(status_code=400, detail="Invalid file type. Only PNG, JPG, and JPEG are supported.")
    
    
    file_path = SHARED_DIR / file.filename
    with file_path.open("wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    
    base64_image = encode_image(str(file_path))

    
    messages = [
        {
            "role": "user",
            "content": [
                {
                    "type": "text",
                    "text": prompt,
                },
                {
                    "type": "image_url",
                    "image_url": {
                        "url": f"data:image/jpeg;base64,{base64_image}",
                    },
                },
            ],
        }
    ]

    try:
        
        response = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=messages,
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error interacting with OpenAI API: {str(e)}")
    finally:
        
        file_path.unlink(missing_ok=True)

    
    print(response.json())
    response_json = json.loads(response.json())
    message_content = response_json["choices"][0]["message"]
    return JSONResponse(content={"response": message_content})
