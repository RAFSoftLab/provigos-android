import org.jetbrains.kotlin.konan.properties.Properties
import java.io.FileInputStream

val secretsPropertiesFile = rootProject.file("secrets.properties")
val secrets = Properties()
secrets.load(FileInputStream(secretsPropertiesFile))

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")
    id("jacoco")
}

@Suppress("UnstableApiUsage")
android {
    namespace = "com.provigos.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.provigos.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        manifestPlaceholders["appAuthRedirectScheme"] = "com.provigos.android"
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${secrets["GOOGLE_CLIENT_ID"]}\"")
        buildConfigField("String", "GITHUB_CLIENT_ID", "\"${secrets["GH_CLIENT_ID"]}\"")
        buildConfigField("String", "GITHUB_CLIENT_SECRET", "\"${secrets["GH_CLIENT_SECRET"]}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_ID", "\"${secrets["SPOTIFY_CLIENT_ID"]}\"")
        buildConfigField("String", "SPOTIFY_CLIENT_SECRET", "\"${secrets["SPOTIFY_CLIENT_SECRET"]}\"")
    }

    testOptions {
        animationsDisabled = true
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    tasks.register<JacocoReport>("jacocoTestReport") {
        dependsOn("testDebugUnitTest")

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        executionData.setFrom(fileTree(layout.buildDirectory).include("/jacoco/testDebugUnitTest.exec"))

        sourceDirectories.setFrom(files("$projectDir/src/main/java"))
        classDirectories.setFrom(
            files(
                fileTree("${layout.buildDirectory}/intermediates/javac/debug") {
                    exclude("**/R.class", "**/R$*.class", "**/BuildConfig.class", "**/Manifest*.*")
                }
            )
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.preference)
    implementation(libs.play.services.base)
    implementation(platform(libs.firebase.bom))
    implementation(libs.org.jacoco.core)

    // TEST
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.contrib)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Koin
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    testImplementation(libs.koin.test)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.adapter.rxjava2)
    implementation("com.squareup.retrofit2", name = "converter-moshi", version = "2.9.0")

    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Moshi
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.moshi.kotlin)
    implementation("com.squareup.moshi", name = "moshi-adapters", version = "1.12.0")

    // Rx
    implementation(libs.rxandroid)
    implementation(libs.rxjava)

    // Stetho
    implementation(libs.stetho)
    implementation(libs.stetho.okhttp3)
    implementation(libs.stetho.urlconnection)
    implementation(libs.stetho.js.rhino)

    // Timber
    implementation(libs.timber)

    // UI
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.drawerlayout)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.mpandroidchart)

    // Fragment
    implementation(libs.androidx.fragment.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)

    // Activity
    implementation(libs.androidx.activity.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.rxjava2)
    ksp(libs.androidx.room.compiler)

    //Health Connect
    implementation(libs.androidx.connect.client)

    //Debug
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Security
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.security.crypto)
    implementation(libs.appauth)
    implementation(libs.java.jwt)

}

kotlin {
    tasks.register("testClasses")
}