@echo off

:: Use argument to decide which build system should be used
if "%1" == "gradle" goto initForGradle
if "%1" == "maven" goto initForMaven
if "%1" == "ant" goto initForAnt
goto invalidInput

:initForGradle
echo "Executing Gradle setup..."
goto initDefault

:initForMaven
echo "Executing Maven setup..."
goto initDefault

:initForAnt
echo "Executing Ant setup..."
::If the directory exists the script has already been executed

::Gets the owncloud-android-library
call git submodule init
call git submodule update

call android.bat update project -p libs/android-support-appcompat-v7-exploded-aar --target android-22
call android.bat update lib-project -p owncloud-android-library
call android.bat update project -p .
call android.bat update project -p oc_jb_workaround
call android.bat update test-project -p tests -m ..

goto complete

:initDefault
call git submodule init
call git submodule update
call android.bat update lib-project -p owncloud-android-library
call android.bat update project -p .
call android.bat update project -p oc_jb_workaround
call android.bat update test-project -p tests -m ..
goto complete

:invalidInput
echo "Input argument invalid."
echo "Usage: %0 [ant | maven | gradle]."
goto exit

:complete
echo "...setup complete."
goto exit

:exit