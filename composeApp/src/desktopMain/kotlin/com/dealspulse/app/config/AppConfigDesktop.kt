package com.dealspulse.app.config

actual fun getSupabaseUrl(): String = System.getenv("SUPABASE_URL") ?: ""
actual fun getSupabaseAnonKey(): String = System.getenv("SUPABASE_ANON_KEY") ?: ""