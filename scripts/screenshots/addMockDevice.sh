#!/bin/bash

cd scripts/screenshots/
for i in $(find ../../fastlane | grep png | grep Screenshots) ; do 
    device=$(echo $i | cut -d"/" -f8 | sed s'#Screenshots##')
    textID=$(echo $i | cut -d"/" -f9 | cut -d"_" -f1,2)
    locale=$(echo $i | cut -d"/" -f6)
    
    # handle some locales different
    case $locale in
        "en-US")
            locale=""
            ;;
        "de-DE")
            locale="-de-rDE"
            ;;
        "es-MX")
            locale="-es-rMX"
            ;;
        "hu-HU")
            locale="-hu-rHU"
            ;;
        "ka-GE")
            locale="-ka-rGE"
            ;;
        "no-NO")
            locale="-nb-rNO"
            ;;
        "pt-BR")
            locale="-pt-rBR"
            ;;
        *)
            locale="-"$(echo $locale | cut -d"-" -f1)
    esac
    
    if [ -e ../../src/main/res/values$locale/strings.xml ] ; then
        text=$(grep $textID ../../src/main/res/values$locale/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    else
        text=""
    fi
    
    # fallback to english if there is not translation
    if [ -z "$text" ]; then
        text=$(grep $textID ../../src/main/res/values/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    fi
    

    sed "s#{image}#$i#;s#{text}#$text#g" $device.svg > temp.svg
    
    if [ $textID == "06_davdroid" ] ; then
        sed "s#display:none#display:visible#" -i temp.svg
    fi
    
    inkscape temp.svg -h 576 -e $i 2>/dev/null
done
