package com.adrorodri.wifidirectexample

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

fun AppCompatActivity.hasPermission(perm: String): Boolean =
    ActivityCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED