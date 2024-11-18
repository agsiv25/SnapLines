./bin/ollama serve &

pid=$!

sleep 5

echo "Pulling bakllava model"
ollama pull bakllava

wait $pid