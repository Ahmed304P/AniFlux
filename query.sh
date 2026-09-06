curl -X POST \
-H "Content-Type: application/json" \
-d '{"query": "query { Media(search: \"Kanojo, Okarishimasu\", type: ANIME) { relations { edges { relationType node { title { english romaji } } } } } }"}' \
https://graphql.anilist.co | jq .
