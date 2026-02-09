package com.nonthakorn.adminnonochanomkaimook

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseConfig {
    val client = createSupabaseClient(
        supabaseUrl = "https://kbcntnjlxszwjjygtpeu.supabase.co/",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImtiY250bmpseHN6d2pqeWd0cGV1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg0NTY2NjQsImV4cCI6MjA4NDAzMjY2NH0.KA4nzIT_9AxoVT4TBcKQq8HvCemRwGCurHaYJnya2Po"
    ) {
        install(Postgrest)
    }
}