plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.android.application")
}

kotlin {
    androidTarget()
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "composeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                
                // Supabase
                implementation("io.github.jan-tennert.supabase:postgrest-kt:1.4.7")
                implementation("io.github.jan-tennert.supabase:realtime-kt:1.4.7")
                implementation("io.github.jan-tennert.supabase:storage-kt:1.4.7")
                implementation("io.github.jan-tennert.supabase:gotrue-kt:1.4.7")
                
                // Ktor for HTTP
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                
                // Navigation
                implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
                implementation("cafe.adriel.voyager:voyager-transitions:1.0.0")
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
                
                // Location services
                implementation("com.google.android.gms:play-services-location:21.0.1")
                
                // Ktor Android
                implementation("io.ktor:ktor-client-android:2.3.7")
            }
        }
        
        val iosMain by creating {
            dependencies {
                // Ktor iOS
                implementation("io.ktor:ktor-client-darwin:2.3.7")
            }
        }
    }
}

android {
    namespace = "com.dealspulse.app"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.dealspulse.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}