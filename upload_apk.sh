#!/bin/bash
echo "Uploading APK to Gofile..."
cp app/build/outputs/apk/debug/app-debug.apk /tmp/AniFlux_3.4.apk
curl -s -F "file=@/tmp/AniFlux_3.4.apk" https://store1.gofile.io/contents/uploadfile > /tmp/apk_url.txt
echo "Upload complete! Result:"
cat /tmp/apk_url.txt
