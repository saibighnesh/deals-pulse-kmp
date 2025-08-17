package com.dealspulse.app.config

expect object AppConfig {
    val supabaseUrl: String
    val supabaseAnonKey: String
    val supabaseServiceKey: String
}