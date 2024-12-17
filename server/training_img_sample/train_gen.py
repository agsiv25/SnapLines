import os
import base64
import requests
import json

# OpenAI API Key
api_key = os.getenv("OPENAI_API_KEY")

# Function to encode the image
def encode_image(image_path):
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')

# Path to your images folder
folder_path = "dataset/images"

headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {api_key}"
}

# Question to ask for each image
question = "what are the two nfl teams in this image, give the answer as (team1, team2)" 
# Function to process each image and get the description
def process_image(image_path, image_name):
    base64_image = encode_image(image_path)
    
    payload = {
        "model": "gpt-4o",
        "messages": [
            {
                "role": "user",
                "content": [
                    {
                        "type": "text",
                        "text": question
                    },
                    {
                        "type": "image_url",
                        "image_url": {
                            "url": f"data:image/jpeg;base64,{base64_image}"
                        }
                    }
                ]
            }
        ],
        "max_tokens": 300
    }

    response = requests.post("https://api.openai.com/v1/chat/completions", headers=headers, json=payload)
    response_json = response.json()
    return response_json['choices'][0]['message']['content']

# List to store all JSON data
all_json_data = []

# Process each image in the folder
for image_name in os.listdir(folder_path):
    if image_name.endswith((".jpg", ".jpeg", ".png")):
        image_path = os.path.join(folder_path, image_name)
        formatted_answers = process_image(image_path, image_name)
        
        json_data = {
            "id": image_name.split('.')[0],
            "image": image_name,
            "conversations": [
                {
                    "from": "human",
                    "value": question
                },
                {
                    "from": "gpt",
                    "value": formatted_answers
                }
            ]
        }
        
        all_json_data.append(json_data)

# Save the results to a JSON file
output_file = "output.json"
with open(output_file, "w") as outfile:
    json.dump(all_json_data, outfile, indent=4)

print(f"Data has been saved to {output_file}")