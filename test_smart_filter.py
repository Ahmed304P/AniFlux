import re

STANDARD_GENRES = {
    "Action": "أكشن",
    "Adventure": "مغامرات",
    "Comedy": "كوميديا",
    "Drama": "دراما",
    "Fantasy": "خيال",
    "Horror": "رعب",
    "Mystery": "غموض",
    "Psychological": "نفسي",
    "Romance": "رومانسية",
    "Sci-Fi": "خيال علمي",
    "Slice of Life": "شريحة من الحياة",
    "Sports": "رياضة",
    "Supernatural": "خارق للطبيعة",
    "Suspense": "تشويق وإثارة",
    "Thriller": "إثارة",
    "Ecchi": "إيتشي",
    "Gourmet": "طبخ وطعام",
    "Mecha": "ميكا وآلات",
    "Music": "موسيقى",
    "Award Winning": "حائز على جوائز"
}

STANDARD_DEMOGRAPHICS = {
    "Shounen": "شونين",
    "Seinen": "سينين",
    "Shoujo": "شوجو",
    "Josei": "جوسي",
    "Kids": "أطفال"
}

APPROVED_THEMES = {
    "School": "مدرسي",
    "Historical": "تاريخي",
    "Military": "عسكري",
    "Super Power": "قوى خارقة",
    "Martial Arts": "فنون قتالية",
    "Isekai": "إيسيكاي",
    "Vampire": "مصاصي دماء",
    "Space": "فضاء",
    "Mythology": "أساطير",
    "Detective": "تحري",
    "Samurai": "ساموراي",
    "Time Travel": "سفر عبر الزمن",
    "Survival": "نجاة",
    "Gore": "دموي",
    "Strategy Game": "ألعاب استراتيجية",
    "Team Sports": "رياضات جماعية"
}

# Junk themes / niches to ignore
BLOCKED_THEMES = {
    "Otaku Culture", "CGDCT", "Adult Cast", "Childcare", "Anthropomorphic",
    "Avant Garde", "Crossdressing", "Delinquents", "Educational", "Gag Humor",
    "High Stakes Game", "Idols Female", "Idols Male", "Iyashikei", "Love Polygon",
    "Love Status Quo", "Magical Sex Shift", "Medical", "Organized Crime",
    "Performing Arts", "Pets", "Racing", "Reincarnation", "Reverse Harem",
    "Romantic Subtext", "Showbiz", "Urban Fantasy", "Video Game", "Villainess",
    "Visual Arts", "Workplace", "Harem", "Parody"
}

def process_mal_genres(genre_list, theme_list, demo_list):
    result = []
    
    # 1. Primary Genres (priority 1)
    for g in genre_list:
        if g in STANDARD_GENRES and STANDARD_GENRES[g] not in result:
            result.append(STANDARD_GENRES[g])
            
    # 2. Approved Major Themes (priority 2, skip junk)
    for t in theme_list:
        if t in APPROVED_THEMES and APPROVED_THEMES[t] not in result:
            result.append(APPROVED_THEMES[t])
            
    # 3. Demographic (priority 3)
    for d in demo_list:
        if d in STANDARD_DEMOGRAPHICS and STANDARD_DEMOGRAPHICS[d] not in result:
            result.append(STANDARD_DEMOGRAPHICS[d])
            
    return result

# Test Sono Bisque Doll:
# Genre: ['Romance']
# Themes: ['Otaku Culture', 'School']
# Demographic: ['Seinen']
res = process_mal_genres(['Romance'], ['Otaku Culture', 'School'], ['Seinen'])
print("Sono Bisque Doll cleaned genres:", res)

# Test JJK:
res_jjk = process_mal_genres(['Action', 'Award Winning', 'Supernatural'], ['School'], ['Shounen'])
print("Jujutsu Kaisen cleaned genres:", res_jjk)

# Test AOT:
res_aot = process_mal_genres(['Action', 'Award Winning', 'Drama', 'Suspense'], ['Gore', 'Military', 'Survival'], ['Shounen'])
print("Attack on Titan cleaned genres:", res_aot)

