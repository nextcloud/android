#!/bin/bash

count=$(grep \.java\" -c "$HOME"/files_added.json)

if [ "$count" -eq 0 ] ; then
    exit 0
else
    echo "New Java files detected! Please use Kotlin for new files. Number of new Java files: $count"
    exit 1
fi

