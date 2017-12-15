#!/bin/sh

cd scripts/screenshots/
for i in $(find ../../fastlane | grep png) ; do 
    device=$(echo $i | cut -d"/" -f8 | sed s'#Screenshots##')
    textID=$(echo $i | cut -d"/" -f9 | cut -d"_" -f1,2)
    locale=$(echo $i | cut -d"/" -f6)
    
    # handle some locales different
    case $locale in
        "en-US")
            locale=""
            ;;
        *)
            locale="-"+$locale
    esac
    
    text=$(grep $textID ../../src/main/res/values$locale/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    
    # fallback to english if there is not translation
    if [ $text == "" ]; then
        text=$(grep $textID ../../src/main/res/values/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    fi
    

    sed "s#{image}#$i#;s#{text}#$text#g" $device.svg > temp.svg
    
    if [ $textID == "06_davdroid" ] ; then
        sed "s#display:none#display:visible#" -i temp.svg
    fi
    
    inkscape temp.svg -e $i
done
