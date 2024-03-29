#!/bin/bash
#
# SPDX-FileCopyrightText: 2021-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: GPL-3.0-or-later

mkdir -p app/build/screenshotSummary/images

scripts/generateScreenshotOverview.sh > app/build/screenshotSummary/summary.html
error=$?

scripts/uploadScreenshotSummary.sh $LOG_USERNAME $LOG_PASSWORD

exit $error
