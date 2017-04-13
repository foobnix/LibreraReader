MY=/home/ivan-dev/git/LirbiReader

tar -cvzhf source.tar.gz \
--exclude=bin \
--exclude=android-* \
--exclude=google-* \
--exclude=Builder \
--exclude=player \
--exclude=cpu_x86 \
--exclude=cpu_all/libs \
--exclude=arm* \
--exclude=project.properties \
--exclude=*.zip \
--exclude=*.tar.gz \
--exclude=*.iml \
--exclude=lint.xml \
--exclude=build.xml \
--exclude=gen \
--exclude=player \
--exclude=.[a-z]* \
--exclude=Issue.com \
--exclude=LibMobi \
--exclude=Z* \
--exclude=*.sh \
../

cp source.tar.gz /home/ivan-dev/Dropbox/FREE_PDF_APK/lirbi-source.tar.gz
rm source.tar.gz