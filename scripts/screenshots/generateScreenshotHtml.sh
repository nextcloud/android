#!/bin/bash

folder=build/screenshotOverview
rm -r $folder
mkdir $folder
file=$folder/overview.html

echo "<html><table>" >> $file

echo "<tr>
<td>Test name</td>
<td>Blue on light</td>
<td>Blue on dark</td>
<td>White on light</td>
<td>White on dark</td>
</tr>" >> $file

for screenshot in $(find screenshots/gplay -type f | grep -v "_dark_" | grep -v "_light_" | sort| head -n1); do
    testName=$(basename $(echo $screenshot | sed s'/\.png//'))
    baseName=$(echo $screenshot | sed s'/\.png//')

    echo "<tr>" >> $file

    #name
    echo "<td>$testName</td>" >> $file

    #base
    cp $baseName.png $folder
    echo "<td><img width='200px' src="$testName.png"></td>" >> $file

    for type in dark_blue light_white dark_white; do
        cp $baseName""_""$type.png $folder
        echo "<td><img width='200px' src=\"$testName""_""$type.png\"></td>" >> $file
    done
    echo "</tr>" >> $file
done

echo "</table></html>" >> $file
echo $file
