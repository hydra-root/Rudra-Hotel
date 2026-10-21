package com.teasupplier.app

/**
 * Project: Hotel Rudra Billing & Management System
 * Author & Programmer: Pavan Pawar
 * Description: Base Application class initializing Hilt dependency injection.
 */

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TeaSupplierApp : Application()
