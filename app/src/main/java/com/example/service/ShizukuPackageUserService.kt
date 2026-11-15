package com.example.service

import android.content.Context
import androidx.annotation.Keep
import com.example.IShizukuPackageService
import kotlin.system.exitProcess

@Keep
class ShizukuPackageUserService : IShizukuPackageService.Stub {

    constructor() : super()

    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context) : super()

    override fun hidePackage(packageName: String): Int {
        if (!isValidPackageName(packageName)) return -1
        val disableCode = runPmCommand("disable-user", "--user", "0", packageName)
        if (disableCode == 0) return 0
        return runPmCommand("hide", packageName)
    }

    override fun unhidePackage(packageName: String): Int {
        if (!isValidPackageName(packageName)) return -1
        val enableCode = runPmCommand("enable", packageName)
        if (enableCode == 0) {
            runPmCommand("unhide", packageName)
            return 0
        }
        return runPmCommand("unhide", packageName)
    }

    override fun isPackageDisabled(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        return runCatching {
            val process = ProcessBuilder("pm", "list", "packages", "-d", packageName)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.lines().any { it.trim() == "package:$packageName" }
        }.getOrDefault(false)
    }

    override fun destroy() {
        exitProcess(0)
    }

    private fun runPmCommand(vararg args: String): Int {
        return runCatching {
            val cmd = listOf("pm") + args.toList()
            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().readText()
            process.waitFor()
        }.getOrDefault(-1)
    }

    private fun isValidPackageName(pkg: String): Boolean =
        pkg.matches(Regex("^[a-zA-Z0-9._]{3,128}$"))
}
