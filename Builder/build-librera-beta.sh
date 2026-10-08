#!/usr/bin/env bash
#./fonts.sh

# every translation must have the same number of lines as the English strings.xml
RES="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )/../app/src/main/res"
if [ ! -f "$RES/values/strings.xml" ]; then
  echo "ERROR: $RES/values/strings.xml not found"
  exit 1
fi
EN_LINES=$(wc -l < "$RES/values/strings.xml" | tr -d " ")
TRANSLATION_ERROR=0
for FILE in "$RES"/values-*/strings.xml; do
  LINES=$(wc -l < "$FILE" | tr -d " ")
  if [ "$LINES" -ne "$EN_LINES" ]; then
    echo "ERROR: $(basename "$(dirname "$FILE")")/strings.xml has $LINES lines, values/strings.xml (en) has $EN_LINES"
    TRANSLATION_ERROR=1
  fi
done
if [ "$TRANSLATION_ERROR" -ne 0 ]; then
  echo "ERROR: not all strings are translated"
  exit 1
fi
echo "Translations OK: $EN_LINES lines"

#/usr/libexec/java_home -V
if [ "$(uname)" == "Darwin" ]; then
  export JAVA_HOME=`/usr/libexec/java_home -v 24`
else
  # librera_java_home in the global ~/.gradle/gradle.properties, else the JDK of Android Studio
  LIBRERA_JAVA_HOME=$(sed -n 's/^librera_java_home=//p' "${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties" | tail -n 1)
  export JAVA_HOME="${LIBRERA_JAVA_HOME:-$HOME/.local/share/JetBrains/Toolbox/apps/android-studio/jbr}"
fi


# The betas of every MuPDF: ./all-beta.sh, or ./all-beta.sh 1.28.5 for one of them. The version
# goes into the names of the APKs and the bundles (-Pmupdf), so the betas sit side by side.
MUPDF_VERSIONS=("$@")
if [ ${#MUPDF_VERSIONS[@]} -eq 0 ]; then
  MUPDF_VERSIONS=(1.28.5)
fi
for MUPDF in "${MUPDF_VERSIONS[@]}"; do
  if [ ! -f "./link_to_mupdf_$MUPDF.sh" ]; then
    echo "ERROR: no MuPDF [$MUPDF], one of:" $(ls link_to_mupdf_*.sh | sed 's/link_to_mupdf_//; s/\.sh//')
    exit 1
  fi
done
echo "MuPDF: ${MUPDF_VERSIONS[*]}"

cd ../

# One gradle run at a time, and nothing is built until the raised version is on disk.
CODE_BEFORE=$(sed -n 's/^appCodeNumber=//p' app/gradle.properties | tr -d ' \r')

./gradlew clean || exit 1
./gradlew incVersion || exit 1
./gradlew updateFDroid || exit 1

CODE_AFTER=$(sed -n 's/^appCodeNumber=//p' app/gradle.properties | tr -d ' \r')
if [ -z "$CODE_AFTER" ] || [ "$CODE_AFTER" = "$CODE_BEFORE" ]; then
  echo "ERROR: incVersion left appCodeNumber at $CODE_BEFORE"
  exit 1
fi
echo "Version raised: $CODE_BEFORE -> $CODE_AFTER"

# One pass per MuPDF, the version is only raised once: every beta is of the same Librera
for MUPDF in "${MUPDF_VERSIONS[@]}"; do
  echo "=================="
  echo "MuPDF: $MUPDF"
  echo "=================="

  ./Builder/link_to_mupdf_$MUPDF.sh || exit 1

  # the native library of the MuPDF before must not end up in these APKs
  ./gradlew clean || exit 1

  ./gradlew assembleProRelease -Pmupdf=$MUPDF || exit 1
  #./gradlew assembleLibreraRelease -Pmupdf=$MUPDF
  ./gradlew assembleFdroidRelease -Pmupdf=$MUPDF || exit 1

  ####################################

  ./gradlew copyApks -Pbeta -Pmupdf=$MUPDF || exit 1
done
./gradlew -stop

####################################


#rm "$BUILDS_DIR"/*-x86*
#rm "$BUILDS_DIR"/*-arm.apk

cd Builder
./remove_all.sh
# The betas are in a folder of their own now: LibreraPro inside librera_builds_dir
./install_all.sh LibreraPro
