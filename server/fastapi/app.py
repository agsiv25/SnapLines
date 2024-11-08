from fastapi import FastAPI, Response
import requests

app = FastAPI()

@app.get('/')
def home():
    return {"hello" : "world"}

@app.get('/prompt')
def ask(prompt :str):
    res  = requests.post('http://ollamav:11434/api/generate', json={
        "prompt": prompt,
        "stream": False,
        "model" : "bakllava"
    })

    return Response(content=res.text, media_type = "application/json")

