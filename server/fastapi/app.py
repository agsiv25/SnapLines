from fastapi import FastAPI, HTTPException, UploadFile, File
from fastapi.responses import JSONResponse, Response
import httpx
from pathlib import Path
import shutil
import base64
import os
import json
import re
import time
from urllib.parse import unquote

app = FastAPI()


team_list = [
    "Arizona Cardinals", "Atlanta Falcons", "Baltimore Ravens", "Buffalo Bills",
    "Carolina Panthers", "Chicago Bears", "Cincinnati Bengals", "Cleveland Browns",
    "Dallas Cowboys", "Denver Broncos", "Detroit Lions", "Green Bay Packers",
    "Houston Texans", "Indianapolis Colts", "Jacksonville Jaguars", "Kansas City Chiefs",
    "Las Vegas Raiders", "Los Angeles Chargers", "Los Angeles Rams", "Miami Dolphins",
    "Minnesota Vikings", "New England Patriots", "New Orleans Saints", "New York Giants",
    "New York Jets", "Philadelphia Eagles", "Pittsburgh Steelers", "San Francisco 49ers",
    "Seattle Seahawks", "Tampa Bay Buccaneers", "Tennessee Titans", "Washington Commanders"
]


short_to_full = {team.split()[-1]: team for team in team_list}

def validate_nfl_teams(input_string):
    pattern = r"^\(([^,]+), ([^)]+)\)$"
    match = re.match(pattern, input_string)

    if not match:
        return None

    team1_short, team2_short = match.groups()
    team1_full = short_to_full.get(team1_short)
    team2_full = short_to_full.get(team2_short)

    if team1_full and team2_full:
        return f"({team1_full}, {team2_full})"
    
    return None

SHARED_DIR = Path("/shared")
SHARED_DIR.mkdir(parents=True, exist_ok=True)

def encode_image(image_path: str) -> str:
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode("utf-8")
    
def validate_nfl_teams(input_string):
    pattern = r"^\(([^,]+), ([^)]+)\)$"
    match = re.match(pattern, input_string)

    if not match:
        return False

    team1, team2 = match.groups()
    return team1 in team_list and team2 in team_list

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

@app.post('/football')
async def football(file: UploadFile = File(...)):

    if not file.filename.lower().endswith(('.png', '.jpg', '.jpeg')):
        raise HTTPException(status_code=400, detail="Invalid file type. Only PNG, JPG, and JPEG are supported.")
    
    file_path = SHARED_DIR / file.filename
    with file_path.open("wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    prompt = f"what are the two nfl teams in this image, give the answer as (team1, team2) using this list of teams {team_list}. Make sure to return the team names exactly as they appear in the list. \"/shared/{file.filename}\""

    try:
        async with httpx.AsyncClient(timeout=600) as client:
            res = await client.post('http://ollama:11434/api/generate', json={
                "prompt": prompt,
                "stream": False,
                "model": "bakllava"
            })
            res.raise_for_status()
    except httpx.RequestError as e:
        return JSONResponse(content={"error": str(e)}, status_code=500)
    

    file_path.unlink(missing_ok=True)

    message_content = json.loads(res.text)["text"]
    
    if validate_nfl_teams(message_content):
        return JSONResponse(content={"response": validate_nfl_teams(message_content)})
    else:
        raise HTTPException(status_code=400, detail="Unable to identify teams.")

