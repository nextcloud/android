/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Jimly Asshiddiqy <jimly.asshiddiqy@accenture.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:Suppress("UnstableApiUsage", "DEPRECATION")

import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import com.karumi.shot.ShotExtension
import org.gradle.internal.jvm.Jvm
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

val shotTest = System.getenv("SHOT_TEST") == "true"
val ciBuild = System.getenv("CI") == "true"
val perfAnalysis = project.hasProperty("perfAnalysis")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.spotless)
    alias(libs.plugins.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.detekt)
    // needed to make renovate run without shot, as shot requires Android SDK
    // https://github.com/pedrovgs/Shot/issues/300
    if (System.getenv("SHOT_TEST") == "true") alias(libs.plugins.shot)
    id("checkstyle")
    id("pmd")
}
apply(from = "${rootProject.projectDir}/jacoco.gradle.kts")

println("Gradle uses Java ${Jvm.current()}")

configurations.configureEach {
    // via prism4j, already using annotations explicitly
    exclude(group = "org.jetbrains", module = "annotations-java5")

    resolutionStrategy {
        force(libs.objenesis)

        eachDependency {
            if (requested.group == "org.checkerframework" && requested.name != "checker-compat-qual") {
                useVersion(libs.versions.checker.get())
                because("https://github.com/google/ExoPlayer/issues/10007")
            } else if (requested.group == "org.jacoco") {
                useVersion(libs.versions.jacoco.get())
            } else if (requested.group == "commons-logging" && requested.name == "commons-logging") {
                useTarget(libs.slfj)
            }
        }
    }
}

// semantic versioning for version code
val versionMajor = 3
val versionMinor = 35
val versionPatch = 0
val versionBuild = 0 // 0-50=Alpha / 51-98=RC / 90-99=stable

val ndkEnv = buildMap {
    file("${project.rootDir}/ndk.env").readLines().forEach {
        val (key, value) = it.split("=")
        put(key, value)
    }
}

val configProps = Properties().apply {
    val file = rootProject.file(".gradle/config.properties")
    if (file.exists()) load(FileInputStream(file))
}

val ncTestServerUsername = configProps["NC_TEST_SERVER_USERNAME"]
val ncTestServerPassword = configProps["NC_TEST_SERVER_PASSWORD"]
val ncTestServerBaseUrl = configProps["NC_TEST_SERVER_BASEURL"]

android {
    // install this NDK version and Cmake to produce smaller APKs. Build will still work if not installed
    ndkVersion = "${ndkEnv["NDK_VERSION"]}"

    namespace = "com.owncloud.android"
    testNamespace = "${namespace}.test"

    androidResources.generateLocaleConfig = true

    defaultConfig {
        applicationId = "com.nextcloud.client"
        minSdk = 27
        targetSdk = 36
        compileSdk = 36

        buildConfigField("boolean", "CI", ciBuild.toString())
        buildConfigField("boolean", "RUNTIME_PERF_ANALYSIS", perfAnalysis.toString())

        javaCompileOptions.annotationProcessorOptions {
            arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
        }

        // arguments to be passed to functional tests
        testInstrumentationRunner = if (shotTest) "com.karumi.shot.ShotTestRunner"
        else "com.nextcloud.client.TestRunner"

        testInstrumentationRunnerArguments += mapOf(
            "TEST_SERVER_URL" to ncTestServerBaseUrl.toString(),
            "TEST_SERVER_USERNAME" to ncTestServerUsername.toString(),
            "TEST_SERVER_PASSWORD" to ncTestServerPassword.toString()
        )
        testInstrumentationRunnerArguments["disableAnalytics"] = "true"

        versionCode = versionMajor * 10000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
        versionName = when {
            versionBuild > 89 -> "${versionMajor}.${versionMinor}.${versionPatch}"
            versionBuild > 50 -> "${versionMajor}.${versionMinor}.${versionPatch} RC" + (versionBuild - 50)
            else -> "${versionMajor}.${versionMinor}.${versionPatch} Alpha" + (versionBuild + 1)
        }

        // adapt structure from Eclipse to Gradle/Android Studio expectations;
        // see http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Configuring-the-Structure

        flavorDimensions += "default"

        buildTypes {
            release {
                buildConfigField("String", "NC_TEST_SERVER_DATA_STRING", "\"\"")
            }

            debug {
                enableUnitTestCoverage = project.hasProperty("coverage")
                resConfigs("xxxhdpi")

                buildConfigField(
                    "String",
                    "NC_TEST_SERVER_DATA_STRING",
                    "\"nc://login/user:${ncTestServerUsername}&password:${ncTestServerPassword}&server:${ncTestServerBaseUrl}\""
                )
            }
        }

        productFlavors {
            // used for f-droid
            register("generic") {
                applicationId = "com.nextcloud.client"
                dimension = "default"
            }

            register("gplay") {
                applicationId = "com.nextcloud.client"
                dimension = "default"
            }

            register("huawei") {
                applicationId = "com.nextcloud.client"
                dimension = "default"
            }

            register("versionDev") {
                applicationId = "com.nextcloud.android.beta"
                dimension = "default"
                versionCode = 20251104
                versionName = "20251104"
            }

            register("qa") {
                applicationId = "com.nextcloud.android.qa"
                dimension = "default"
                versionCode = 1
                versionName = "1"
            }
        }
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            if (this is ApkVariantOutputImpl) this.outputFileName = "${this.baseName}-${this.versionCode}.apk"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        animationsDisabled = true
    }

    // adapt structure from Eclipse to Gradle/Android Studio expectations;
    // see http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Configuring-the-Structure
    packaging.resources {
        excludes.addAll(listOf("META-INF/LICENSE*", "META-INF/versions/9/OSGI-INF/MANIFEST*"))
        pickFirsts.add("MANIFEST.MF") // workaround for duplicated manifest on some dependencies
    }

    buildFeatures {
        buildConfig = true
        dataBinding = true
        viewBinding = true
        aidl = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkGeneratedSources = true
        disable.addAll(
            listOf(
                "MissingTranslation",
                "GradleDependency",
                "VectorPath",
                "IconMissingDensityFolder",
                "IconDensities",
                "GoogleAppIndexingWarning",
                "MissingDefaultResource",
                "InvalidPeriodicWorkRequestInterval",
                "StringFormatInvalid",
                "MissingQuantity",
                "IconXmlAndPng",
                "SelectedPhotoAccess",
                "UnsafeIntentLaunch"
            )
        )
        htmlOutput = layout.buildDirectory.file("reports/lint/lint.html").get().asFile
        htmlReport = true
    }

    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }

}

kapt.useBuildCache = true

ksp.arg("room.schemaLocation", "$projectDir/schemas")

kotlin.compilerOptions.jvmTarget.set(JvmTarget.JVM_17)

spotless.kotlin {
    target("**/*.kt")
    ktlint()
}

detekt.config.setFrom("detekt.yml")

if (shotTest) configure<ShotExtension> {
    showOnlyFailingTestsInReports = ciBuild
    // CI environment renders some shadows slightly different from local VMs
    // Add a 0.5% tolerance to account for that
    tolerance = if (ciBuild) 0.1 else 0.0
}


spotbugs {
    ignoreFailures = true // should continue checking
    effort = Effort.MAX
    reportLevel = Confidence.valueOf("MEDIUM")
}

tasks.register<Checkstyle>("checkstyle") {
    configFile = file("${rootProject.projectDir}/checkstyle.xml")
    setConfigProperties(
        "checkstyleSuppressionsPath" to file("${rootProject.rootDir}/suppressions.xml").absolutePath
    )
    source("src")
    include("**/*.java")
    exclude("**/gen/**")
    classpath = files()
}

tasks.register<Pmd>("pmd") {
    ruleSetFiles = files("${rootProject.rootDir}/ruleset.xml")
    ignoreFailures = true // should continue checking
    ruleSets = emptyList()

    source("src")
    include("**/*.java")
    exclude("**/gen/**")

    reports {
        xml.outputLocation.set(layout.buildDirectory.file("reports/pmd/pmd.xml").get().asFile)
        html.outputLocation.set(layout.buildDirectory.file("reports/pmd/pmd.html").get().asFile)
    }
}

tasks.withType<SpotBugsTask>().configureEach {
    val variantNameCap = name.replace("spotbugs", "")
    val variantName = variantNameCap.substring(0, 1).lowercase() + variantNameCap.substring(1)
    dependsOn("compile${variantNameCap}Sources")

    classes = fileTree(
        layout.buildDirectory.get().asFile.toString() +
            "/intermediates/javac/${variantName}/compile${variantNameCap}JavaWithJavac/classes/"
    )
    excludeFilter.set(file("${project.rootDir}/scripts/analysis/spotbugs-filter.xml"))

    reports.create("xml") {
        required.set(true)
    }
    reports.create("html") {
        required.set(true)
        outputLocation.set(layout.buildDirectory.file("reports/spotbugs/spotbugs.html"))
        setStylesheet("fancy.xsl")
    }
}

// Run the compiler as a separate process
tasks.withType<JavaCompile>().configureEach {
    options.isFork = true

    // Enable Incremental Compilation
    options.isIncremental = true
}

tasks.withType<Test>().configureEach {
    // Run tests in parallel
    maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)

    // increased logging for tests
    testLogging.events("passed", "skipped", "failed")
}

tasks.named("check").configure {
    dependsOn("checkstyle", "spotbugsGplayDebug", "pmd", "lint", "spotlessKotlinCheck", "detekt")
}

dependencies {
    // region Nextcloud library
    implementation(libs.android.library) {
        exclude(group = "org.ogce", module = "xpp3") // unused in Android and brings wrong Junit version
    }
    // endregion

    // region Splash Screen
    implementation(libs.splashscreen)
    // endregion

    // region Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.material.icons.core)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    // endregion

    // region Media3
    implementation(libs.bundles.media3)
    // endregion

    // region Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    androidTestImplementation(libs.room.testing)
    // endregion

    // region Espresso
    androidTestImplementation(libs.bundles.espresso)
    // endregion

    // region Glide
    implementation(libs.glide)
    ksp(libs.ksp)
    // endregion

    // region UI
    implementation(libs.bundles.ui)
    // endregion

    // region Worker
    implementation(libs.work.runtime)
    implementation(libs.work.runtime.ktx)
    // endregion

    // region Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.service)
    implementation(libs.lifecycle.runtime.ktx)
    // endregion

    // region JUnit
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.rules)
    androidTestImplementation(libs.runner)
    androidTestUtil(libs.orchestrator)
    androidTestImplementation(libs.core.ktx)
    androidTestImplementation(libs.core.testing)
    // endregion

    // region other libraries
    compileOnly(libs.org.jbundle.util.osgi.wrapped.org.apache.http.client)
    implementation(libs.commons.httpclient.commons.httpclient) // remove after entire switch to lib v2
    implementation(libs.jackrabbit.webdav) // remove after entire switch to lib v2
    implementation(libs.constraintlayout)
    implementation(libs.legacy.support.v4)
    implementation(libs.material)
    implementation(libs.disklrucache)
    implementation(libs.juniversalchardet) // need this version for Android <7
    compileOnly(libs.annotations)
    implementation(libs.commons.io)
    implementation(libs.eventbus)
    implementation(libs.ez.vcard)
    implementation(libs.nnio)
    implementation(libs.bcpkix.jdk18on)
    implementation(libs.gson)
    implementation(libs.sectioned.recyclerview)
    implementation(libs.photoview)
    implementation(libs.android.gif.drawable)
    implementation(libs.qrcodescanner) // "com.github.blikoon:QRCodeScanner:0.1.2"
    implementation(libs.flexbox)
    implementation(libs.androidsvg)
    implementation(libs.annotation)
    implementation(libs.emoji.google)
    // endregion

    // region AppScan, document scanner not available on FDroid (generic) due to OpenCV binaries
    "gplayImplementation"(project(":appscan"))
    "huaweiImplementation"(project(":appscan"))
    "qaImplementation"(project(":appscan"))
    // endregion

    // region SpotBugs
    spotbugsPlugins(libs.findsecbugs.plugin)
    spotbugsPlugins(libs.fb.contrib)
    // endregion

    // region Dagger
    implementation(libs.dagger)
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    ksp(libs.dagger.compiler)
    ksp(libs.dagger.processor)
    // endregion

    // region Crypto
    implementation(libs.conscrypt.android)
    // endregion

    // region Library
    implementation(libs.library)
    // endregion

    // region Shimmer
    implementation(libs.loaderviewlibrary)
    // endregion

    // region Markdown rendering
    implementation(libs.bundles.markdown.rendering)
    kapt(libs.prism4j.bundler)
    // endregion

    // region Image cropping / rotation
    implementation(libs.android.image.cropper)
    // endregion

    // region Maps
    implementation(libs.osmdroid.android)
    // endregion

    // region iCal4j
    implementation(libs.ical4j) {
        listOf("org.apache.commons", "commons-logging").forEach { groupName -> exclude(group = groupName) }
    }
    // endregion

    // region LeakCanary
    if (perfAnalysis) debugImplementation(libs.leakcanary)
    // endregion

    // region Local Unit Test
    testImplementation(libs.bundles.unit.test)
    // endregion

    // region Mocking support
    androidTestImplementation(libs.bundles.mocking)
    // endregion

    // region UIAutomator
    // UIAutomator - for cross-app UI tests, and to grant screen is turned on in Espresso tests
    // androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0"
    // fix conflict in dependencies; see http://g.co/androidstudio/app-test-app-conflict for details
    // androidTestImplementation("com.android.support:support-annotations:${supportLibraryVersion}"
    androidTestImplementation(libs.screengrab)
    // endregion

    // region Kotlin
    implementation(libs.kotlin.stdlib)
    // endregion

    // region Stateless
    implementation(libs.stateless4j)
    // endregion

    // region Google Play dependencies, upon each update first test: new registration, receive push
    "gplayImplementation"(libs.bundles.gplay)
    // endregion

    // region UI
    implementation(libs.ui)
    // endregion

    // region Image loading
    implementation(libs.coil)
    // endregion

    // kotlinx.serialization
    implementation(libs.kotlinx.serialization.json)
}
