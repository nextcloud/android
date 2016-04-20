#!/bin/bash -e


function initDefault {
    git submodule init
    git submodule update
    android update lib-project -p owncloud-android-library
    android update project -p .
    android update project -p oc_jb_workaround
    android update test-project -p tests -m ..
}

#No args
if [ $# -lt 1 ]; then
        echo "No args found"
        echo "Usage : $0 [gradle]"        exit
fi

#checking args
case "$1" in

    "gradle")  echo  "Creating gradle environment"
        initDefault
        ;;

    *)  echo "Argument not recognized"
        echo "Usage : $0 [gradle]"       ;;
esac

exit
