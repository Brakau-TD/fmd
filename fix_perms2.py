import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

old_eff = """        } else {
            // Auto start background tracking service once permissions are valid
            viewModel.startService(context)
        }"""

new_eff = """        } else {
            // Auto start background tracking service once permissions are valid
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                bgLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                viewModel.startService(context)
            }
        }"""

content = content.replace(old_eff, new_eff)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
