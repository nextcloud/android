#!/bin/bash
#
# SPDX-FileCopyrightText: 2021-2024 Nextcloud GmbH and Nextcloud contributors
# SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
# SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only

mkdir -p app/build/screenshotSummary/images

scripts/generateScreenshotOverview.sh > app/build/screenshotSummary/summary.html
error=$?

scripts/uploadScreenshotSummary.sh $LOG_USERNAME $LOG_PASSWORD

exit $error
