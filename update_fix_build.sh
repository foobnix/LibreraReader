#
for d in * ; do
if [[ -d $d ]]; then
    #echo folder "$d"
    if [[ $d == google* ]] || [[ $d == android* ]] ;     
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
