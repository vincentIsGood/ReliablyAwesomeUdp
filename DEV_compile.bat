@echo off

set first=src/com/vincentcodes/io/*.java src/com/vincentcodes/test/manual/*.java
:: .java files are in encoding UTF-8
javac -encoding UTF-8 --release 10 -d classes -cp ./lib/*;./src/ %first%

pause