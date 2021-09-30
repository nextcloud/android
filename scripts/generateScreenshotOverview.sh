#!/bin/bash
#
# Nextcloud Android client application
#
# @author Tobias Kaminsky
# Copyright (C) 2021 Tobias Kaminsky
# Copyright (C) 2021 Nextcloud GmbH
#  
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#  
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU Affero General Public License for more details.
#  
# You should have received a copy of the GNU Affero General Public License
# along with this program. If not, see <https://www.gnu.org/licenses/>.
#

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
    cp $image build/screenshotSummary/images/
    
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
        if [ ! -e ./build/reports/shot/verification/images/$name ]; then
            echo "<span style='color: red'>✘</span>"
            error=$((error + 1))
        elif [ -e ./build/reports/shot/verification/images/diff_$name ]; then
            # file with "diff_" prefix
            cp ./build/reports/shot/verification/images/diff_$name build/screenshotSummary/images/
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
