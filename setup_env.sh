#!/bin/bash

android update project -p actionbarsherlock
android update project -p .
cd tests
android update test-project -m .. -p .
