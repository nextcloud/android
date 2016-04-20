@echo off

:: Use argument to decide which build system should be used
if "%1" == "gradle" goto initForGradle
goto invalidInput

:initForGradle
echo "Executing Gradle setup..."
goto initDefault

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
echo "Usage: %0 [gradle]."goto exit

:complete
echo "...setup complete."
goto exit

:exit