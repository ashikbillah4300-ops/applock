package com.example.data.disguise

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisguiseManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val defaultAlias = ComponentName(context, "com.example.LauncherDefaultAlias")
    private val calculatorAlias = ComponentName(context, "com.example.LauncherCalculatorAlias")

    fun isCalculatorDisguiseEnabled(): Boolean {
        val pm = context.packageManager
        return pm.getComponentEnabledSetting(calculatorAlias) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }

    fun setCalculatorDisguise(enabled: Boolean) {
        val pm = context.packageManager
        if (enabled) {
            pm.setComponentEnabledSetting(
                calculatorAlias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                defaultAlias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            pm.setComponentEnabledSetting(
                defaultAlias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                calculatorAlias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
