# Nextcloud - Android Client
#
# SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
# SPDX-License-Identifier: AGPL-3.0-or-later

# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Silence missing-class warnings from optional transitive dependencies and Android/JDK API gaps.
-dontwarn com.gemalto.jp2.JP2Decoder
-dontwarn com.gemalto.jp2.JP2Encoder
-dontwarn java.time.zone.ZoneRulesProvider
-dontwarn org.joda.convert.ToString

# Commons HttpClient is still used at runtime, so keep these packages from being stripped or renamed.
-keep class org.apache.commons.httpclient.* { *; }
-keep class org.apache.commons.httpclient.auth.** { *; }

# Some libraries load .properties resources relative to their package.
# Keeping the package names preserves Class.getResourceAsStream(...) lookups after shrinking.
-keeppackagenames org.apache.jackrabbit.webdav
-keeppackagenames net.fortuna.ical4j
-keep class org.apache.commons.httpclient.cookie.** { *; }
