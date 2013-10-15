git submodule init
git submodule update
call android.bat update project -p actionbarsherlock\library --target 1
call android.bat update project -p . --target 1
copy /Y third_party\android-support-library\android-support-v4.jar actionbarsherlock\library\libs\android-support-v4.jar
cd tests
call android.bat update test-project -m .. -p .
cd ..
