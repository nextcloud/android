#!/bin/bash

git submodule init
git submodule update
android update project -p actionbarsherlock/library
android update project -p .

