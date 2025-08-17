package com.dealspulse.app.data

import com.dealspulse.app.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object Supa {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = AppConfig.supabaseUrl,
        supabaseKey = AppConfig.supabaseKey
    ) {
        install(Postgrest)
        install(GoTrue)
        install(Realtime)
        install(Storage)
    }
    
    val auth get() = client.auth
    val database get() = client.postgrest
    val realtime get() = client.realtime
    val storage get() = client.storage
}