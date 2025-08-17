plugins {
	kotlin("multiplatform") version "2.0.20"
	id("org.jetbrains.compose") version "1.6.11"
	id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
	id("org.jetbrains.kotlin.plugin.compose") version "2.0.20"
}

kotlin {
	// Add a JVM target to allow building in this environment; common code is shared.
	jvm("desktop")
	jvmToolchain(21)

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(compose.runtime)
				implementation(compose.foundation)
				implementation(compose.material)

				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
				implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

				// Ktor Client core + content negotiation
				implementation("io.ktor:ktor-client-core:2.3.12")
				implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
				implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
			}
		}
		val commonTest by getting
		val desktopMain by getting {
			dependencies {
				implementation(compose.desktop.currentOs)
				// Ktor engine for JVM Desktop
				implementation("io.ktor:ktor-client-cio:2.3.12")
			}
		}
		val desktopTest by getting
	}
}

compose.desktop {
	application {
		mainClass = "com.dealspulse.app.MainKt"
	}
}