#! /bin/sh
rm /vbshared/MSDCollector-debug.apk 
#rm -r tmp
set -e
#mkdir tmp
#mkdir tmp/lib
#mkdir tmp/lib/armeabi
#cp ~/msd/parser/compile4/parser/* tmp/lib/armeabi/
ant "-Djava.compilerargs=-Xlint:unchecked -Xlint:deprecation" -Dndk.dir=/home/user/android-ndk-r10 debug
#cp bin/qgsmmap-debug.apk /vbshared/
#cd tmp;aapt add ../bin/MSDCollector-debug.apk lib/armeabi/*;cd ..
cp bin/MSDCollector-debug.apk /vbshared/