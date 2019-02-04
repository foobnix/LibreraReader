#!/bin/bash

LIBS=/home/ivan-dev/git/LibreraReader/EBookDroid/libs

cd $LIBS

wget -nc http://central.maven.org/maven2/org/greenrobot/eventbus/3.1.1/eventbus-3.1.1.jar
wget -nc http://central.maven.org/maven2/org/greenrobot/greendao/3.2.2/greendao-3.2.2.jar
wget -nc http://central.maven.org/maven2/org/greenrobot/greendao-api/3.2.2/greendao-api-3.2.2.jar
wget -nc http://central.maven.org/maven2/org/jsoup/jsoup/1.11.3/jsoup-1.11.3.jar
wget -nc http://central.maven.org/maven2/com/googlecode/juniversalchardet/juniversalchardet/1.0.3/juniversalchardet-1.0.3.jar
wget -nc http://central.maven.org/maven2/com/squareup/okhttp3/okhttp/3.12.1/okhttp-3.12.1.jar
wget -nc http://repo.spring.io/plugins-release/com/burgstaller/okhttp-digest/1.18/okhttp-digest-1.18.jar
wget -nc http://central.maven.org/maven2/com/squareup/okio/okio/1.17.3/okio-1.17.3.jar
wget -nc http://central.maven.org/maven2/com/github/joniles/rtfparserkit/1.13.0/rtfparserkit-1.13.0.jar

wget -nc https://github.com/CloudRail/cloudrail-si-android-sdk/blob/master/cloudrail-si-release.aar?raw=true -O CloudRail.aar
unzip -p CloudRail.aar classes.jar >cloudrail.jar
rm CloudRail.aar