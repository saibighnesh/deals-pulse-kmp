package com.dealspulse.app.config

import com.dealspulse.app.BuildConfig

actual object AppConfig {
    actual val supabaseUrl: String = BuildConfig.SUPABASE_URL
    actual val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY
    actual val supabaseServiceKey: String = BuildConfig.SUPABASE_SERVICE_KEY
}