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
        "en-GB")
            locale="-b+en+001"
            ;;
        "de-DE")
            locale="-de"
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
        "pt-PT")
            locale="-pt-rPT"
            ;;
        "bg-BG")
            locale="-bg-rBG"
            ;;
        "fi-FI")
            locale="-fi-rFI"
            ;;
        "uk-UK")
            locale=""
            ;;
        "ja-JP")
            locale="-ja-rJP"
            ;;
        "lt-LT")
            locale="-lt-rLT"
            ;;
        "zh-HK")
            locale="-zh-rCN"
            ;;
        "zk-CN")
            locale="-zh-rCN"
            ;;
        "id-ID")
            locale="-in"
            ;;
        "cs-CZ")
            locale="-cs-rCZ"
            ;;
        *)
            locale="-"$(echo $locale | cut -d"-" -f1)
    esac

    if [ -e ../../src/main/res/values$locale/strings.xml ] ; then
        heading=$(grep $textID"_heading" ../../src/main/res/values$locale/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
        subline=$(grep $textID"_subline" ../../src/main/res/values$locale/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    else
        heading=""
        subline=""
    fi

    # fallback to english if there is not translation
    if [ -z "$heading" ]; then
        heading=$(grep $textID"_heading" ../../src/main/res/values/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    fi

    if [ -z "$subline" ]; then
        subline=$(grep $textID"_subline" ../../src/main/res/values/strings.xml | cut -d">" -f2 | cut -d"<" -f1 | sed s'#\&amp;#\\&#')
    fi


    sed "s#{image}#$i#;s#{heading}#$heading#;s#{subline}#$subline#g" $device.svg > temp.svg

    if [ $textID == "06_davdroid" ] ; then
        sed "s#display:none#display:visible#" -i temp.svg
    fi

    inkscape temp.svg -h 576 -e $i 2>/dev/null
done
