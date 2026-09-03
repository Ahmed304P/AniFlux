import re

with open('app/src/main/java/com/example/ui/components/ExoPlayerView.kt', 'r') as f:
    content = f.read()

# Add ContextWrapper import
if 'import android.content.ContextWrapper' not in content:
    content = content.replace('import android.content.Context', 'import android.content.Context\nimport android.content.ContextWrapper')

# Add findActivity extension function
find_activity_func = """
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
"""
if 'fun Context.findActivity()' not in content:
    content += find_activity_func

# Replace context as? Activity with context.findActivity()
content = content.replace('val activity = context as? Activity', 'val activity = context.findActivity()')

# We also should hide the system UI when in fullscreen
# Add core window imports
if 'import androidx.core.view.WindowCompat' not in content:
    content = content.replace('import android.app.Activity', 'import android.app.Activity\nimport androidx.core.view.WindowCompat\nimport androidx.core.view.WindowInsetsCompat\nimport androidx.core.view.WindowInsetsControllerCompat')

# Update LaunchedEffect for fullscreen
old_fullscreen_effect = """    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }"""

new_fullscreen_effect = """    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }"""

content = content.replace(old_fullscreen_effect, new_fullscreen_effect)

with open('app/src/main/java/com/example/ui/components/ExoPlayerView.kt', 'w') as f:
    f.write(content)

