#!/bin/bash
app=$1
echo "app=$app"
cp ./Dockerfile $app/build/libs/
cd $app/build/libs
docker build -t $app --build-arg app=$app .
docker images>./cmd.out
imageid=""
while read line
do
   echo "$line" #输出整行内容
   IFS=" "
   arr=($line)
   if [ "$app" == "${arr[0]}" ]; then
        imageid=${arr[2]}
        echo "-----------$app.imageid=$imageid"
        break;
   fi
done<./cmd.out
echo "$imageid"
docker tag $imageid registry.cn-hangzhou.aliyuncs.com/gehc/$app:latest
docker push registry.cn-hangzhou.aliyuncs.com/gehc/$app:latest