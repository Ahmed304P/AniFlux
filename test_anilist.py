import requests
import json

graphqlQuery = """
query ($search: String) {
  Page(page: 1, perPage: 1) {
    media(search: $search, type: ANIME) {
      id
      title { romaji english }
      studios(isMain: true) { nodes { name } }
    }
  }
}
"""
url = "https://graphql.anilist.co"
response = requests.post(url, json={'query': graphqlQuery, 'variables': {'search': 'Naruto'}})
print(response.status_code)
print(json.dumps(response.json(), indent=2))
