ant clean build

#mvn deploy:deploy-file -Dfile=../build/lib/gwt-elemental.jar -DpomFile=pom.xml \
#  -Durl=https://oss.sonatype.org/content/repositories/snapshots/ -DrepositoryId=sonatype-nexus-snapshots -Dclassifier= -DuniqueVersion=false

mvn install:install-file -Dfile=../build/lib/gwt-elemental.jar -DpomFile=pom.xml
