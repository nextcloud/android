#!/bin/bash

./gradlew combinedTestReport
status=$?

curl -o codecov.sh https://codecov.io/bash
bash ./codecov.sh -t fc506ba4-33c3-43e4-a760-aada38c24fd5

exit $status
