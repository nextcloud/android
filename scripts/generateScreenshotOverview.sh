#!/bin/bash
#
# SPDX-FileCopyrightText: 2021-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

error=0
total=0

cp scripts/screenshotCombinations scripts/screenshotCombinations_
grep -v "#" scripts/screenshotCombinations_ > scripts/screenshotCombinations
rm scripts/screenshotCombinations_

echo '<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="utf-8"/>
</head>'

echo "<table>"
echo "<tr><td style='width:150px'>Original</td>"
while read line; do
    echo "<td style='width:150px'>$line</td>"
done < scripts/screenshotCombinations
echo "</tr>"

#for image in ./build/reports/shot/verification/images/*.png ; do
for image in $(/bin/ls -1 ./screenshots/gplay/debug/*.png | grep -v _dark_ | grep -v _light_) ; do
    cp $image app/build/screenshotSummary/images/
    
    echo "<tr style='height:200px'>"
    echo "<td><a target='_blank' href=\"images/$(basename $image)\"><img width=100px src=\"images/$(basename $image)\"/></a></td>"

    while read line; do
        echo "<td>"
        
        mode=$(echo $line | cut -d" " -f1)
        color=$(echo $line | cut -d" " -f2)
        total=$((total + 1))
        
        if [ $mode = "light" -a $color = "blue" ]; then
            name=$(basename $image)
        else
            name=$(basename $image| sed s"/\.png/_${mode}_$color\.png/")
        fi
        
        # if image does not exist
        if [ ! -e ./app/build/reports/shot/verification/images/$name ]; then
            echo "<span style='color: red'>✘</span>"
            error=$((error + 1))
        elif [ -e ./app/build/reports/shot/verification/images/diff_$name ]; then
            # file with "diff_" prefix
            cp ./app/build/reports/shot/verification/images/diff_$name build/screenshotSummary/images/
            echo "<a target='_blank' href=\"images/diff_$name\"><img width=100px src=\"images/diff_$name\"/></a>"
            error=$((error + 1))
        else 
            echo "✔"
        fi
        
        echo "</td>"
    done < scripts/screenshotCombinations
    
    echo "</tr>"
done

echo "</table>"

echo "ERROR: $error / $total"
echo "</html>"
