#!/bin/bash
set -e
root=$(cd "$(dirname "$0")";cd ..;pwd)

# 用optipng无损压缩项目中所有png图片，
find "$root/skWeiChatBaidu/src/main/res" -name "*.png" -exec optipng {} \;
