plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "be.chvp.nanoledger"
    compileSdk = 34

    defaultConfig {
        applicationId = "be.chvp.nanoledger"
        minSdk = 21
        targetSdk = 34
        versionCode = 20100
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs +=
            arrayOf(
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            )
    }
    lint {
        quiet = true
        disable.addAll(
            arrayOf(
                "GradleDependency",
                "NewerVersionAvailable",
                "ObsoleteLintCustomCheck",
                "SimpleDateFormat",
            ),
        )
        checkAllWarnings = true
        ignoreWarnings = false
        warningsAsErrors = true
        textReport = true
        explainIssues = !project.hasProperty("isCI")
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.lint {
    dependsOn(tasks.ktlintCheck)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    android.set(true)
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar)
    implementation(libs.activity.compose)
    implementation(libs.activity.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material3)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.core.ktx)
    implementation(libs.hilt)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.viewmodel.ktx)
    ksp(libs.hilt.compiler)
    testImplementation(kotlin("test"))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
