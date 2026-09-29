import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Mirrors the pattern apps/web uses with .env.local: secrets/config live in a
// gitignored local file, never committed, never hardcoded in source.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localProp(key: String, default: String): String =
    (localProperties.getProperty(key) ?: System.getenv(key))?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.costiq.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.costiq.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Public-safe Supabase project ref; matches apps/web/.env.local's
        // NEXT_PUBLIC_SUPABASE_URL. The anon key is genuinely public-safe
        // (designed to ship in client bundles) but still lives in
        // local.properties, not source, so per-environment overrides don't
        // require touching this file.
        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProp("SUPABASE_URL", "https://nyfvtdqpasvxtaygzllz.supabase.co")}\""
        )
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProp("SUPABASE_ANON_KEY", "")}\"")

        // OAuth Web Client ID from the Google Cloud project backing Supabase's
        // Google provider — public-safe (it's the audience of the ID token,
        // not a secret), same one apps/web's Supabase Google provider uses.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProp("GOOGLE_WEB_CLIENT_ID", "1020258442575-l6o3kndj7iagrkbu12e4esb9sjnnijat.apps.googleusercontent.com")}\""
        )
    }

    signingConfigs {
        create("release") {
            // Keystore lives outside source control (apps/android/.gitignore);
            // path/passwords come from local.properties, same pattern as the
            // API/Supabase config above. Losing this keystore means Play Store
            // updates to this app package can never be published again.
            val storeFileName = localProp("RELEASE_STORE_FILE", "")
            if (storeFileName.isNotBlank()) {
                storeFile = rootProject.file(storeFileName)
                storePassword = localProp("RELEASE_STORE_PASSWORD", "")
                keyAlias = localProp("RELEASE_KEY_ALIAS", "")
                keyPassword = localProp("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        debug {
            // 10.0.2.2 is the emulator's alias for the host machine's
            // localhost — matches `pnpm worker` running on :8787 outside
            // the emulator. Override in local.properties for a physical
            // device on the same LAN, e.g. API_BASE_URL=http://192.168.1.23:8787
            buildConfigField("String", "API_BASE_URL", "\"${localProp("API_BASE_URL", "http://10.0.2.2:8787")}\"")
            // Web dashboard (apps/web, `next dev -p 3003`), opened in the device browser.
            buildConfigField("String", "WEB_BASE_URL", "\"${localProp("WEB_BASE_URL", "http://10.0.2.2:3003")}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"${localProp("API_BASE_URL_RELEASE", "https://api.costiq.app")}\"")
            buildConfigField("String", "WEB_BASE_URL", "\"${localProp("WEB_BASE_URL_RELEASE", "https://expense-app-web-woad.vercel.app")}\"")
            if (localProp("RELEASE_STORE_FILE", "").isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth.kt)
    implementation(libs.ktor.client.okhttp)

    testImplementation("junit:junit:4.13.2")
}
