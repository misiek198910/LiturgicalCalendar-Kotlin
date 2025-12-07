import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}
val props = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { props.load(it) }
}

android {
    namespace = "mivs.liturgicalcalendar"

    compileSdk = 36

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "mivs.kalendarz_liturgiczny"
        minSdk = 27
        targetSdk = 36
        versionCode = 38
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val admobAppId = props.getProperty("ADMOB_APP_ID", "BRAK_ID")
        manifestPlaceholders["admobAppId"] = admobAppId
    }

    signingConfigs {
        create("release") {
            // Czytamy z 'props'
            storeFile = file(props.getProperty("MYAPP_RELEASE_STORE_FILE", "brak-sciezki"))
            storePassword = props.getProperty("MYAPP_RELEASE_STORE_PASSWORD", "")
            keyAlias = props.getProperty("MYAPP_RELEASE_KEY_ALIAS", "")
            keyPassword = props.getProperty("MYAPP_RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {

            val bannerId = props.getProperty("AD_BANNER_ID", "BRAK_ID")
            val adStartId = props.getProperty("AD_START_UNIT_ID", "BRAK_ID")

            buildConfigField("String", "AD_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "AD_START_UNIT_ID", "\"$adStartId\"")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // Testowe ID od Google
            buildConfigField("String", "AD_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "AD_START_UNIT_ID", "\"ca-app-pub-3940256099942544/3419835294\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.applandeo.calendar)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.jsoup)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.billing)
}