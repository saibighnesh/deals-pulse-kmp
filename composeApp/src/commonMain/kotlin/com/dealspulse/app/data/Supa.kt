package com.dealspulse.app.data

import com.dealspulse.app.config.AppConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object Supa {
    
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    val client = createSupabaseClient(
        supabaseUrl = AppConfig.supabaseUrl,
        supabaseKey = AppConfig.supabaseAnonKey
    ) {
        install(GoTrue) {
            httpClient = this@Supa.httpClient
        }
        install(Postgrest) {
            httpClient = this@Supa.httpClient
        }
        install(Realtime) {
            httpClient = this@Supa.httpClient
        }
        install(Storage) {
            httpClient = this@Supa.httpClient
        }
    }
    
    val auth get() = client.auth
    val postgrest get() = client.postgrest
    val realtime get() = client.realtime
    val storage get() = client.storage
}