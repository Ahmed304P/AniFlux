import re

with open("app/src/main/java/com/example/ui/components/ShimmerEffect.kt", "r") as f:
    content = f.read()

target = """            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )"""

replacement = """            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )"""

content = content.replace(target, replacement, 1)

with open("app/src/main/java/com/example/ui/components/ShimmerEffect.kt", "w") as f:
    f.write(content)
