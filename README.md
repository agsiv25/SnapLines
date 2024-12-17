# SnapLines

To start server run:
```
docker compose up --build
```
Wait for both ollama and fastapi server to finishing building(ollama may take 10+ minutes to build)
FastAPI server is exposed at localhost 8000
Go to localhost:8000/docs to manually prompt LLM

To expose to client, use port forwarding through VS code or localhost.run
