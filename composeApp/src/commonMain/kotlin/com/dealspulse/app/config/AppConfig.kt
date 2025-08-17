package com.dealspulse.app.config

expect object AppConfig {
    val supabaseUrl: String
    val supabaseKey: String
    val isDevelopment: Boolean
}