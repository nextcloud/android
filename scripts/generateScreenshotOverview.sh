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

echo "<table>"
echo "<tr><td style='width:150px'>Original</td>"
while read line; do
    echo "<td style='width:150px'>$line</td>"
done < scripts/screenshotCombinations
echo "</tr>"

#for image in ./build/reports/shot/verification/images/*.png ; do
for image in $(/bin/ls -1 ./screenshots/gplay/debug/*.png | grep -v _dark_ | grep -v _light_) ; do
    cp $image build/screenshotSummary/
    
    echo "<tr style='height:200px'>"
    echo "<td><a target='_blank' href=\"$(basename $image)\"><img width=100px src=\"$(basename $image)\"/></a></td>"

    while read line; do
        echo "<td>"
        
        mode=$(echo $line | cut -d" " -f1)
        color=$(echo $line | cut -d" " -f2)
        name=$(basename $image| sed s"/\.png/_light_$color\.png/")
        
        # if image does not exist
        if [ ! -e ./build/reports/shot/verification/images/$name ]; then
            echo "✘"
            error=$((error + 1))
        elif [ -e ./build/reports/shot/verification/images/diff_$name ]; then
            # file with "diff_" prefix
            cp ./build/reports/shot/verification/images/diff_$name build/screenshotSummary
            echo "<a target='_blank' href=\"diff_$name\"><img width=100px src=\"diff_$name\"/></a>"
            error=$((error + 1))
        else 
            echo "✔"
        fi
        
        echo "</td>"
    done < scripts/screenshotCombinations
    
    echo "</tr>"
done

echo "</table>"

echo "ERROR: $error"
exit $error
