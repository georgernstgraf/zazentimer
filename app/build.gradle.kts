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

val rootDirStr = project.rootDir.absolutePath

fun exec(vararg cmd: String): Int =
    ProcessBuilder(*cmd)
        .inheritIO()
        .start()
        .waitFor()

fun execCapture(vararg cmd: String): String =
    ProcessBuilder(*cmd)
        .redirectErrorStream(true)
        .start()
        .inputStream
        .bufferedReader()
        .readText()
        .trim()

fun fileStartsWith(
    file: File,
    prefix: ByteArray,
): Boolean {
    if (!file.exists() || file.length() < prefix.size) return false
    val buf = ByteArray(prefix.size)
    file.inputStream().use { it.read(buf) }
    return buf.contentEquals(prefix)
}

tasks.register("pullDeviceDb") {
    description = "Pull consistent SQLite DB via in-app backup ZIP from device/emulator"
    group = "prisma"
    dependsOn("assembleDebug")
    notCompatibleWithConfigurationCache(
        "Uses ADB to communicate with external device",
    )
    doLast {
        val root = File(rootDirStr, "prisma")
        val scriptDir = File(rootDirStr, "scripts")
        val startEmulator = File(scriptDir, "start-emulator.sh")
        val stopEmulator = File(scriptDir, "stop-emulator.sh")
        val apkFile = File(rootDirStr, "app/build/outputs/apk/debug/app-debug.apk")
        val appId = "at.priv.graf.zazentimer.debug"
        val activity = "$appId/at.priv.graf.zazentimer.ZazenTimerActivity"

        var emulatorStarted = false
        try {
            // ── 1. Device detection ──
            val rawDevices = execCapture("adb", "devices")
            println("ADB devices:\n$rawDevices")
            val deviceCount = rawDevices.lines().count { it.contains("\tdevice") }
            if (deviceCount == 0) {
                println("No ADB device found — starting emulator")
                val ec = exec("bash", startEmulator.absolutePath, "35")
                if (ec != 0) throw GradleException("Emulator start failed (exit=$ec)")
                emulatorStarted = true
            } else {
                println("ADB device found ($deviceCount device(s)) — using it")
            }

            // ── 2. Install APK ──
            println("Installing APK (${apkFile.length()} bytes)...")
            val installEc = exec("adb", "install", "-r", apkFile.absolutePath)
            if (installEc != 0) throw GradleException("adb install failed (exit=$installEc)")
            val packages = execCapture("adb", "shell", "pm", "list", "packages", appId)
            if (!packages.contains(appId)) {
                throw GradleException("Package $appId not found after install")
            }
            println("Install verified: $appId")

            // ── 3. Trigger backup ──
            println("Starting app with create_backup intent...")
            exec("adb", "shell", "am", "force-stop", appId)
            exec("adb", "logcat", "-c")
            exec("adb", "shell", "am", "start", "-n", activity, "--es", "create_backup", "true")

            // ── 4. Wait for app to finish ──
            println("Waiting for app to create backup and exit...")
            val deadline = System.currentTimeMillis() + 60_000
            var appExited = false
            while (System.currentTimeMillis() < deadline) {
                val pid = execCapture("adb", "shell", "pidof", appId)
                if (pid.isEmpty()) {
                    appExited = true
                    break
                }
                Thread.sleep(1000)
            }
            if (!appExited) {
                println("App did not exit within 60s — dumping logcat tail:")
                val logcat = execCapture("adb", "logcat", "-d", "-t", "30")
                println(logcat)
                throw GradleException(
                    "App still running after 60s — backup likely failed (see logcat above)",
                )
            }

            // ── 5. Check backup on device ──
            val deviceZipPath = "cache/zazentimer_backup.zip"
            val stat = execCapture("adb", "shell", "run-as", appId, "stat", deviceZipPath)
            println("Device backup stat: $stat")
            if (!stat.contains("regular file") && !stat.contains("Size:")) {
                throw GradleException("Backup ZIP not found on device: $stat")
            }

            // ── 6. Pull backup ZIP to /tmp/ ──
            val zipFile = File("/tmp/zazentimer_backup.zip")
            println("Pulling backup ZIP from device to ${zipFile.absolutePath}...")
            val pullEc = exec("adb", "pull", "/data/data/$appId/$deviceZipPath", zipFile.absolutePath)
            // fallback: run-as pull if adb pull fails (non-root device)
            if (pullEc != 0 || !zipFile.exists() || zipFile.length() <= 22L) {
                println("adb pull failed or empty, falling back to run-as...")
                ProcessBuilder(
                    "adb",
                    "exec-out",
                    "run-as",
                    appId,
                    "cat",
                    deviceZipPath,
                ).redirectOutput(zipFile)
                    .start()
                    .waitFor()
            }

            if (!fileStartsWith(zipFile, "PK".toByteArray())) {
                throw GradleException(
                    "Downloaded file is not a valid ZIP (${zipFile.length()} bytes, " +
                        "magic=${zipFile.readBytes().take(4).toByteArray().contentToString()})",
                )
            }
            println("Backup ZIP downloaded: ${zipFile.length()} bytes")

            // ── 7. Extract database ──
            println("Extracting database from backup ZIP...")
            val dbFile = File(root, "device_db.sqlite")
            val unzipEc = exec("unzip", "-o", zipFile.absolutePath, "zentimer", "-d", root.absolutePath)
            if (unzipEc != 0) throw GradleException("unzip failed (exit=$unzipEc)")
            val extractedDb = File(root, "zentimer")
            if (!extractedDb.exists()) throw GradleException("Extracted database file 'zentimer' not found in ZIP")
            extractedDb.renameTo(dbFile)
            println("Database extracted: ${dbFile.length()} bytes")

            if (!fileStartsWith(dbFile, "SQLite format 3\u0000".toByteArray())) {
                throw GradleException("Extracted file is not a valid SQLite database")
            }
            println("Database validated: SQLite format 3")
        } finally {
            if (emulatorStarted) {
                println("Stopping emulator...")
                exec("bash", stopEmulator.absolutePath)
            }
        }
    }
}

tasks.register("prismaRefreshSchema") {
    description = "Run prisma db pull to populate current/ from device database"
    group = "prisma"
    dependsOn("pullDeviceDb")
    notCompatibleWithConfigurationCache(
        "Runs external Deno/Prisma CLI process",
    )
    doLast {
        val root = File(rootDirStr, "prisma")
        val currentDir = File(root, "current")
        ProcessBuilder("deno", "-A", "prisma", "db", "pull", "--force")
            .directory(currentDir)
            .inheritIO()
            .start()
            .waitFor()
    }
}

tasks.register("prismaValidateTranslationsSchema") {
    description = "Validate translations Prisma schema"
    group = "prisma"
    notCompatibleWithConfigurationCache(
        "Runs prisma validate",
    )
    doLast {
        val schemaPath = File(rootDirStr, "prisma/translations/schema.prisma").absolutePath
        val proc =
            ProcessBuilder(
                "deno",
                "run",
                "-A",
                "npm:prisma@6.19.2",
                "validate",
                "--schema",
                schemaPath,
            ).inheritIO().start()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            throw GradleException("Translation schema validation failed")
        }
    }
}

tasks.register("prismaCheckSchema") {
    description = "Check device schema matches desired migration schema (drift detection)"
    group = "prisma"
    dependsOn("prismaRefreshSchema")
    notCompatibleWithConfigurationCache(
        "Runs external diff process",
    )
    doLast {
        val root = File(rootDirStr, "prisma")
        val desiredPath = File(root, "desired/schema.prisma").absolutePath
        val currentPath = File(root, "current/schema.prisma").absolutePath
        val proc =
            ProcessBuilder("diff", "-u", desiredPath, currentPath)
                .inheritIO()
                .start()
        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            throw GradleException("Schema drift detected: current/ differs from desired/")
        }
    }
}
