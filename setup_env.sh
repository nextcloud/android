#!/bin/bash -e


#Repository
ActionBarSherlockRepo="https://github.com/JakeWharton/ActionBarSherlock.git"

#Directory for actionbarsherlock
DIRECTORY="actionbarsherlock"

#Commit for version 4.2 of actionbar sherlock
COMMIT="90939dc3925ffaaa0de269bbbe1b35e274968ea1"


function initDefault {
    git submodule init
    git submodule update
    android update lib-project -p owncloud-android-library
    android update project -p .
    android update project -p oc_jb_workaround
    android update test-project -p tests -m ..
}

function initForAnt {
    #If the directory exists the script has already been executed
    if [ ! -d "$DIRECTORY" ]; then

        #Gets the owncloud-android-library
        git submodule init
        git submodule update

        #Clones the actionbarsherlock and checks-out the right release (4.2.0)
        git clone $ActionBarSherlockRepo $DIRECTORY
        cd $DIRECTORY
        git checkout $COMMIT
        cd ../

        #As default it updates the ant scripts
        android update project -p "$DIRECTORY"/library -n ActionBarSherlock --target android-19
        android update lib-project -p owncloud-android-library
        android update project -p .
        android update project -p oc_jb_workaround
        cp third_party/android-support-library/android-support-v4.jar actionbarsherlock/library/libs/android-support-v4.jar
        android update test-project -p tests -m ..
    fi
}

#No args
if [ $# -lt 1 ]; then
        echo "No args found"
        echo "Usage : $0 [gradle | maven | ant]"
        exit
fi

#checking args
case "$1" in

    "ant")  
        echo "Creating Ant environment"
        initForAnt
        ;;

    "gradle")  echo  "Creating gradle environment"
        initDefault
        ;;

    "maven")  echo  "Creating maven environment"
        initDefault
        ;;

    *)  echo "Argument not recognized"
        echo "Usage : $0 [gradle | maven | ant]"
       ;;
esac

exit
