package com.dealspulse.app.config

actual object AppConfig {
    // These would be set in your BuildConfig or local.properties
    // For now, using placeholder values - replace with your actual Supabase credentials
    actual val supabaseUrl: String = "YOUR_SUPABASE_URL"
    actual val supabaseAnonKey: String = "YOUR_SUPABASE_ANON_KEY"
    actual val supabaseServiceKey: String = "YOUR_SUPABASE_SERVICE_KEY"
}