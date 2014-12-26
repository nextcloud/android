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
if not exist .\actionbarsherlock (

    ::Gets the owncloud-android-library
    call git submodule init
    call git submodule update
    
    ::Clones the actionbarsherlock and checks-out the right release (4.2.0)
    git clone "https://github.com/JakeWharton/ActionBarSherlock.git" "actionbarsherlock"
    cd "actionbarsherlock"
    git checkout "90939dc3925ffaaa0de269bbbe1b35e274968ea1"
    cd ../

    call android.bat update project -p actionbarsherlock/library -n ActionBarSherlock --target android-19
    call android.bat update lib-project -p owncloud-android-library
    call android.bat update project -p .
    call android.bat update project -p oc_jb_workaround
    copy /Y third_party\android-support-library\android-support-v4.jar actionbarsherlock\library\libs\android-support-v4.jar
    call android.bat update test-project -p tests -m ..
)
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