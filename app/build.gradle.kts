plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "me.mondiversi.spacecompass"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "me.mondiversi.spacecompass"
        minSdk = 26
        targetSdk = 37
        versionCode = 20
        versionName = "1.1.3"
        buildConfigField("String", "DISPLAY_VERSION", "\"1.1.3\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes { release { optimization { enable = true } } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { compose = true; buildConfig = true }
    bundle { language { enableSplit = false } }
}
dependencies {
    implementation("io.github.cosinekitty:astronomy:2.1.19")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Localization tests read XML directly, so resource edits must invalidate their cached results.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    inputs.files(fileTree("src/main/res") { include("**/*.xml") })
        .withPropertyName("localizationResources")
        .withPathSensitivity(org.gradle.api.tasks.PathSensitivity.RELATIVE)
}
