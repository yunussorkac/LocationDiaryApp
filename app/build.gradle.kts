plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.serialization)
    id ("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

}

android {
    namespace = "com.app.mapory"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.app.mapory"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"

        }
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.material)
    implementation(libs.kotlinx.serialization.json)

    implementation (libs.androidx.datastore.preferences)
    implementation (libs.gson)

    implementation (libs.firebase.auth)
    implementation (libs.firebase.messaging)
    implementation (libs.firebase.firestore)
    implementation (libs.firebase.storage)
    implementation (libs.firebase.analytics)
    implementation (libs.firebase.crashlytics)
    implementation (libs.firebase.config.ktx)

    implementation (libs.koin.androidx.compose)
    implementation (libs.koin.android)
    implementation (libs.koin.androidx.navigation)
    testImplementation(libs.koin.test.junit4)
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)

    implementation (libs.kotlinx.coroutines.play.services)

    implementation (libs.accompanist.systemuicontroller)

    implementation(libs.androidx.runtime.livedata)

    implementation (libs.motiontoast)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation (libs.play.services.location)

    implementation (libs.coil)
    implementation (libs.coil.compose)
    implementation (libs.coil.network.okhttp)

    implementation (libs.androidx.navigation.compose)
    implementation (libs.google.accompanist.pager)

    implementation (libs.androidx.media3.exoplayer)
    implementation (libs.androidx.media3.ui)

    implementation (libs.places)
    implementation(libs.zoomable)
    implementation ("io.github.grizzi91:bouquet:1.1.2")

    implementation ("org.apache.poi:poi:5.2.3")
    implementation ("org.apache.poi:poi-ooxml:5.2.3")
    implementation ("org.apache.poi:poi-scratchpad:5.2.3")

}