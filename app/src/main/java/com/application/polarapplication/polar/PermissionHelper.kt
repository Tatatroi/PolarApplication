package com.application.polarapplication.polar

import android.Manifest
import android.app.Activity
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    private val permissionsBelowAndroid12 = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val permissionsAndroid12Plus = listOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    )

    /**
     * Returns lists of permissions based on Android version.
     */
    private fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsAndroid12Plus
        } else {
            permissionsBelowAndroid12
        }
    }

    /**
     * Check if all required permissions are granted.
     */
    fun hasAllPermissions(activity: Activity): Boolean {
        return getRequiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request all permissions needed for Bluetooth LE.
     */
    fun requestAllPermissions(activity: Activity, requestCode: Int = 1001) {
        val missing = getRequiredPermissions().filter { perm ->
            ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, missing.toTypedArray(), requestCode)
        }
    }
}
