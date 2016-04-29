#!/bin/bash -e

function initDefault {
    git submodule init
    git submodule update
}

echo  "Creating development environment setup..."
initDefault
echo "...setup complete."

exit
