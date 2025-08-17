package com.dealspulse.app.config

actual object AppConfig {
    actual val supabaseUrl: String = "https://your-project.supabase.co"
    actual val supabaseKey: String = "your-anon-key"
    actual val isDevelopment: Boolean = true
}