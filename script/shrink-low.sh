#!/bin/bash
set -e
root=$(cd "$(dirname "$0")";cd ..;pwd)

# 删除存在高分辨率图片的低分辨率版本，
cd "$root/skWeiChatBaidu/src/main/res/mipmap-xxhdpi/"
find . -type f \
  -exec rm -f ../mipmap-hdpi/{} \; \
  -exec rm -f ../mipmap-mdpi/{} \; \
  -exec rm -f ../mipmap-xhdpi/{} \;

cd "$root/skWeiChatBaidu/src/main/res/drawable-xxhdpi/"
find . -type f \
  -exec rm -f ../drawable-hdpi/{} \; \
  -exec rm -f ../drawable-mdpi/{} \; \
  -exec rm -f ../drawable-xhdpi/{} \;
