import urllib.request
import json

try:
    url = "https://api.jikan.moe/v4/anime?q=One%20Piece&limit=10"
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib.request.urlopen(req)
    data = json.loads(response.read().decode('utf-8'))
    print(len(data.get('data', [])))
    print("Success")
except Exception as e:
    print(f"Error: {e}")
