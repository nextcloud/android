#!/bin/bash

file=/tmp/screenshotOverview-$(date +%F-%H-%M-%S)

echo "<html><table>" >> $file

echo "<tr>
<td>Test name</td>
<td>Blue on light</td>
<td>Blue on dark</td>
<td>White on light</td>
<td>White on dark</td>
</tr>" >> $file

for screenshot in $(find screenshots/gplay -type f | grep -v "_dark_" | grep -v "_light_" | sort); do
    echo "<tr>" >> $file
    #name
    echo "<td>$screenshot (base)</td>" >> $file

    #base
    echo "<td><img width='200px' src="$(pwd)/$screenshot"></td>" >> $file

    baseName=$(echo $screenshot | sed s'/\.png//')

    for type in dark_blue light_white dark_white; do
        echo "<td><img width='200px' src=\"$(pwd)/$baseName""_""$type.png\"></td>" >> $file
    done
    echo "</tr>" >> $file
done

echo "</table></html>" >> $file
echo $file
