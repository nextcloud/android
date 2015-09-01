#!/bin/bash -e


function initDefault {
    git submodule init
    git submodule update
    android update lib-project -p owncloud-android-library
    android update project -p .
    android update project -p oc_jb_workaround
    android update test-project -p tests -m ..
}

function initForAnt {

	#Gets the owncloud-android-library
	git submodule init
	git submodule update

	#Prepare project android-support-appcompat-v7 ; JAR file is not enough, includes resources
	android update lib-project -p libs/android-support-appcompat-v7-exploded-aar --target android-22
	
	#As default it updates the ant scripts
	android update lib-project -p owncloud-android-library
	android update project -p .
	android update project -p oc_jb_workaround
	android update test-project -p tests -m ..
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
