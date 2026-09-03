#!/bin/bash
echo "Uploading APK..."
curl -s -F "reqtype=fileupload" -F "time=12h" -F "fileToUpload=@app/build/outputs/apk/debug/app-debug.apk" https://litterbox.catbox.moe/resources/internals/api.php > /tmp/apk_url.txt
echo "Upload complete! URL:"
cat /tmp/apk_url.txt
