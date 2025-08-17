package com.dealspulse.app.config

object AppConfig {
	// For now, keep defaults empty; platform actuals or build configs should set these appropriately.
	val supabaseUrl: String get() = getSupabaseUrl()
	val supabaseAnonKey: String get() = getSupabaseAnonKey()
}

expect fun getSupabaseUrl(): String
expect fun getSupabaseAnonKey(): String