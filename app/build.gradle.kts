import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

abstract class GitHashSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        val process =
            ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
                .directory(File(System.getProperty("user.dir")))
                .start()
        process.waitFor()
        return process.inputStream
            .bufferedReader()
            .readText()
            .trim()
    }
}

abstract class VersionTagSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        val dir = File(System.getProperty("user.dir"))
        ProcessBuilder("git", "fetch", "--tags", "--quiet")
            .directory(dir)
            .start()
            .waitFor()
        val process =
            ProcessBuilder("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
                .directory(dir)
                .start()
        process.waitFor()
        val tag =
            process.inputStream
                .bufferedReader()
                .readText()
                .trim()
        return if (tag.startsWith("v")) tag.substring(1) else "0.0.0"
    }
}

abstract class CommitCountSource : ValueSource<String, ValueSourceParameters.None> {
    override fun obtain(): String {
        val tagProcess =
            ProcessBuilder("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
                .directory(File(System.getProperty("user.dir")))
                .start()
        tagProcess.waitFor()
        val tag =
            tagProcess.inputStream
                .bufferedReader()
                .readText()
                .trim()
        if (!tag.startsWith("v")) return "0"
        val countProcess =
            ProcessBuilder("git", "rev-list", "$tag..HEAD", "--count")
                .directory(File(System.getProperty("user.dir")))
                .start()
        countProcess.waitFor()
        return countProcess.inputStream
            .bufferedReader()
            .readText()
            .trim()
    }
}

plugins {
    id("com.android.application")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "at.priv.graf.zazentimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "at.priv.graf.zazentimer"
        minSdk = 23
        targetSdk = 36
        versionCode =
            if (project.hasProperty("versionCode")) {
                project.property("versionCode").toString().toInt()
            } else {
                val versionFromTag = providers.of(VersionTagSource::class.java) {}.get().trim()
                val baseVersion = versionFromTag.substringBefore("-")
                val parts = baseVersion.split(".")
                val major = parts.getOrElse(0) { "0" }.toInt()
                val minor = parts.getOrElse(1) { "0" }.toInt()
                val patch = parts.getOrElse(2) { "0" }.toInt()
                val commits =
                    providers
                        .of(CommitCountSource::class.java) {}
                        .get()
                        .trim()
                        .toInt()
                major * 1000000 + minor * 10000 + patch * 100 + commits
            }
        versionName =
            if (project.hasProperty("versionName")) {
                project.property("versionName").toString()
            } else {
                providers.of(VersionTagSource::class.java) {}.get().trim()
            }

        testInstrumentationRunner = "at.priv.graf.zazentimer.HiltTestRunner"
        testInstrumentationRunnerArguments["testTimeoutSeconds"] = "120"

        val gitHash = providers.of(GitHashSource::class.java) {}.get().trim()
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")

        val commitCount = providers.of(CommitCountSource::class.java) {}.get().trim()
        val versionDisplay = if (commitCount == "0") versionName else "$versionName+$commitCount"
        buildConfigField("String", "VERSION_DISPLAY", "\"$versionDisplay\"")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        execution = "HOST"
        animationsDisabled = true
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    // Removed explicitApiWarning() as it is unnecessary for an application
    // and causes hundreds of non-idiomatic warnings.

    testFixtures {
        enable = true
    }
}

ktlint {
    version.set("1.6.0")
}

detekt {
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.10.0")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.navigation:navigation-fragment:2.9.7")
    implementation("androidx.navigation:navigation-ui:2.9.7")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.59.2")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.59.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.14.4")
    testImplementation("io.mockk:mockk-android:1.14.4")
    testImplementation("androidx.room:room-testing:2.8.4")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    implementation("androidx.test.espresso:espresso-idling-resource:3.7.0")

    testFixturesImplementation("androidx.test.espresso:espresso-core:3.7.0")
    testFixturesImplementation("androidx.test.espresso:espresso-contrib:3.7.0")
    testFixturesImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    testFixturesImplementation("androidx.recyclerview:recyclerview:1.4.0")

    testImplementation(testFixtures(project(":app")))
    androidTestImplementation(testFixtures(project(":app")))
}
