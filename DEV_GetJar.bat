@echo off

set jarname=udplibrary_v1.0
set structure=com/vincentcodes/io/*
set additional_source=com/vincentcodes/test/*

:: copy my own libraries (error if not found)
:: cp -r lib/com/ .

:: with Manifest
:: cd classes
:: jar -cvfm %jarname%.jar Manifest.txt %structure%
:: mv %jarname%.jar ..

:: rm -r ../com/

:: without Manifest
cd classes
jar -cvf %jarname%.jar %structure%
mv %jarname%.jar ..

cd ../src
jar -cvf %jarname%-sources.jar %structure% %additional_source%
mv %jarname%-sources.jar ..

pause