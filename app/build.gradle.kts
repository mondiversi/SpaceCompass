import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Private Dropbox configuration is never part of the repository or APK.
val releaseSigningDirectory =
    providers.environmentVariable("MONDIVERSI_SIGNING_DIR").orNull
        ?.takeIf { it.isNotBlank() }
        ?.let {
            File(it).also { path ->
                require(path.isAbsolute) { "MONDIVERSI_SIGNING_DIR must be an absolute path" }
            }
        }
        ?: File(System.getProperty("user.home"), "Dropbox/Mondiversi/UvirSigning")
val releaseSigningSources = listOf(
    File(releaseSigningDirectory, "signing.properties"),
    rootProject.file("signing.properties")
).filter { it.isFile }
val releaseSigningProperties = Properties().apply {
    releaseSigningSources.forEach { source -> source.inputStream().use(::load) }
}
fun signingCredential(environmentName: String, propertyName: String): String? =
    providers.environmentVariable(environmentName).orNull?.takeIf { it.isNotEmpty() }
        ?: releaseSigningProperties.getProperty(propertyName)
val releaseStorePassword = signingCredential("SPACE_COMPASS_STORE_PASSWORD", "storePassword")
val releaseKeyAlias = signingCredential("SPACE_COMPASS_KEY_ALIAS", "keyAlias")
val releaseKeyPassword = signingCredential("SPACE_COMPASS_KEY_PASSWORD", "keyPassword")
val releaseStoreOverride = providers.environmentVariable("SPACE_COMPASS_KEYSTORE").orNull
    ?.takeIf { it.isNotBlank() }
    ?: releaseSigningProperties.getProperty("storeFile")?.trim()?.takeIf { it.isNotEmpty() }
val releaseKeystoreFile = releaseStoreOverride?.let {
    File(it).also { path -> require(path.isAbsolute) { "Signing key override must be an absolute path" } }
} ?: File(releaseSigningDirectory, "uvir-release.jks")
val releaseSigningConfigured = releaseSigningSources.isNotEmpty() ||
    listOf(releaseStorePassword, releaseKeyAlias, releaseKeyPassword, releaseStoreOverride).any { it != null }
if (releaseSigningConfigured) {
    require(listOf(releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { !it.isNullOrEmpty() }) {
        "Private signing configuration requires storePassword, keyAlias and keyPassword"
    }
}

android {
    namespace = "me.mondiversi.spacecompass"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "me.mondiversi.spacecompass"
        minSdk = 26
        targetSdk = 37
        versionCode = 48
        versionName = "1.4.10"
        buildConfigField("String", "DISPLAY_VERSION", "\"1.4.10\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (releaseSigningConfigured) {
            create("distribution") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        release {
            if (releaseSigningConfigured) signingConfig = signingConfigs.getByName("distribution")
            optimization { enable = true }
        }
    }
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
