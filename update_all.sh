git clone https://github.com/dandar3/android-support-v4
git clone https://github.com/dandar3/android-support-compat
git clone https://github.com/dandar3/android-support-annotations
git clone https://github.com/dandar3/android-arch-lifecycle-runtime
git clone https://github.com/dandar3/android-arch-lifecycle-common
git clone https://github.com/dandar3/android-arch-core-common
git clone https://github.com/dandar3/android-support-media-compat
git clone https://github.com/dandar3/android-support-core-utils
git clone https://github.com/dandar3/android-support-core-ui
git clone https://github.com/dandar3/android-support-fragment
git clone https://github.com/dandar3/android-support-v7-appcompat
git clone https://github.com/dandar3/android-support-vector-drawable
git clone https://github.com/dandar3/android-support-animated-vector-drawable
git clone https://github.com/dandar3/android-support-v7-recyclerview
git clone https://github.com/dandar3/android-support-v7-cardview

git clone https://github.com/dandar3/android-google-play-services-ads google-play-services-ads
git clone https://github.com/dandar3/android-google-play-services-ads-lite google-play-services-ads-lite
git clone https://github.com/dandar3/android-google-play-services-basement google-play-services-basement
git clone https://github.com/dandar3/android-google-play-services-gass google-play-services-gass
git clone https://github.com/dandar3/android-google-play-services-tasks google-play-services-tasks

#git clone https://github.com/dandar3/android-google-firebase-analytics google-firebase-analytics
#git clone https://github.com/dandar3/android-google-firebase-analytics-impl google-firebase-analytics-impl
#git clone https://github.com/dandar3/android-google-firebase-common google-firebase-common
#git clone https://github.com/dandar3/android-google-firebase-iid google-firebase-iid

for d in * ; do
if [[ -d $d ]]; then
  
    if [[ $d == "google*" ]] || [[ $d == "android*" ]] ;     
then
    echo "[$d]"
    rm $d/build.xml
    #rm $d/proguard-project.txt
    sed -i -e 's/proguard.config/#proguard.config/g' $d/project.properties
    echo '<?xml version="1.0" encoding="UTF-8"?>
	<project name="'$d'" default="help">

       <property file="../Builder/ant.properties" />
    <property file="../Builder/local.properties" />
    
    <loadproperties srcFile="project.properties" />

    <!-- quick check on sdk.dir -->
    <fail
            message="sdk.dir is missing. Make sure to generate local.properties using android update project or to inject it through an env var"
            unless="sdk.dir"
    />
    
    <import file="custom_rules.xml" optional="true" />
    <import file="${sdk.dir}/tools/ant/build.xml" />


</project>

	' >> $d/build.xml 
  
  #git commit -am "init..."
fi
  fi
done
