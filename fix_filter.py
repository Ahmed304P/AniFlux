import re

with open('app/src/main/java/com/example/ui/components/FilterBottomSheet.kt', 'r') as f:
    content = f.read()

# Fix TextButton closing parenthesis
content = content.replace(
"""                    Text(
                        text = "الغاء",
                        color = Color.Gray,
                        fontSize = 16.sp
                )""",
"""                    Text(
                        text = "الغاء",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }""")

content = content.replace(
"""                    Text(
                        text = "تطبيق",
                        color = AniFluxCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                )""",
"""                    Text(
                        text = "تطبيق",
                        color = AniFluxCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }""")

content = content.replace(
"""                    Text(
                        text = item,
                        color = if (isSelected) AniFluxCyan else Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                )
            }
        }""",
"""                    Text(
                        text = item,
                        color = if (isSelected) AniFluxCyan else Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }""")

with open('app/src/main/java/com/example/ui/components/FilterBottomSheet.kt', 'w') as f:
    f.write(content)
print("FilterBottomSheet fixed")
