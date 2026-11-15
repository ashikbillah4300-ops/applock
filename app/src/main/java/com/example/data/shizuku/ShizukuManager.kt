package com.example.data.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import com.example.BuildConfig
import com.example.IShizukuPackageService
import com.example.service.ShizukuPackageUserService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import javax.inject.Inject
import javax.inject.Singleton

enum class ShizukuStatus {
    NOT_INSTALLED,
    NOT_RUNNING,
    PERMISSION_REQUIRED,
    READY
}

sealed interface ShizukuOpResult {
    data object Success : ShizukuOpResult
    data class Failure(val reason: String) : ShizukuOpResult
}

@Singleton
class ShizukuManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _status = MutableStateFlow(computeCurrentStatus())
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private val bindMutex = Mutex()
    private var boundService: IShizukuPackageService? = null
    private var pendingConnection: CompletableDeferred<IShizukuPackageService?>? = null

    private val userServiceArgs: Shizuku.UserServiceArgs by lazy {
        Shizuku.UserServiceArgs(
            ComponentName(context, ShizukuPackageUserService::class.java)
        )
            .daemon(false)
            .version(BuildConfig.VERSION_CODE)
            .processNameSuffix("shizuku_pkg")
            .tag("GlassLockPkgService")
            .debuggable(BuildConfig.DEBUG)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = if (binder != null && binder.isBinderAlive) {
                IShizukuPackageService.Stub.asInterface(binder)
            } else {
                null
            }
            boundService = service
            pendingConnection?.complete(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            pendingConnection?.complete(null)
            refreshStatus()
        }
    }

    init {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { refreshStatus() }
            Shizuku.addBinderDeadListener {
                boundService = null
                refreshStatus()
            }
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
                    refreshStatus()
                }
            }
        }
    }

    fun refreshStatus(): ShizukuStatus {
        val updated = computeCurrentStatus()
        _status.value = updated
        return updated
    }

    private fun computeCurrentStatus(): ShizukuStatus {
        if (!isShizukuInstalled()) return ShizukuStatus.NOT_INSTALLED
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        if (!alive) return ShizukuStatus.NOT_RUNNING
        val granted = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        return if (granted) ShizukuStatus.READY else ShizukuStatus.PERMISSION_REQUIRED
    }

    fun isShizukuInstalled(): Boolean {
        return runCatching {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(SHIZUKU_PACKAGE, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(SHIZUKU_PACKAGE, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun requestPermission() {
        if (refreshStatus() == ShizukuStatus.PERMISSION_REQUIRED) {
            runCatching {
                if (!Shizuku.shouldShowRequestPermissionRationale()) {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                } else {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                }
            }
        }
    }

    fun createOpenShizukuOrStoreIntent(): Intent {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        return (launchIntent ?: Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$SHIZUKU_PACKAGE")
        )).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createDeveloperSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    suspend fun hidePackage(packageName: String): ShizukuOpResult = withContext(Dispatchers.IO) {
        val current = refreshStatus()
        if (current != ShizukuStatus.READY) {
            return@withContext ShizukuOpResult.Failure(statusErrorMessage(current))
        }
        val service = getOrBindService()
            ?: return@withContext ShizukuOpResult.Failure("Shizuku service stopped or failed to bind. Restart Shizuku and retry.")
        val code = runCatching { service.hidePackage(packageName) }.getOrElse {
            boundService = null
            return@withContext ShizukuOpResult.Failure("Shizuku connection lost (${it.message ?: "binder error"}). Restart Shizuku.")
        }
        if (code == 0) {
            ShizukuOpResult.Success
        } else {
            ShizukuOpResult.Failure("Package manager rejected hide for $packageName (exit code $code).")
        }
    }

    suspend fun unhidePackage(packageName: String): ShizukuOpResult = withContext(Dispatchers.IO) {
        val current = refreshStatus()
        if (current != ShizukuStatus.READY) {
            return@withContext ShizukuOpResult.Failure(statusErrorMessage(current))
        }
        val service = getOrBindService()
            ?: return@withContext ShizukuOpResult.Failure("Shizuku service stopped or failed to bind. Restart Shizuku and retry.")
        val code = runCatching { service.unhidePackage(packageName) }.getOrElse {
            boundService = null
            return@withContext ShizukuOpResult.Failure("Shizuku connection lost (${it.message ?: "binder error"}). Restart Shizuku.")
        }
        if (code == 0) {
            ShizukuOpResult.Success
        } else {
            ShizukuOpResult.Failure("Package manager failed to re-enable $packageName (exit code $code).")
        }
    }

    suspend fun isPackageDisabledViaShizuku(packageName: String): Boolean? = withContext(Dispatchers.IO) {
        if (refreshStatus() != ShizukuStatus.READY) return@withContext null
        val service = getOrBindService() ?: return@withContext null
        runCatching { service.isPackageDisabled(packageName) }.getOrNull()
    }

    private suspend fun getOrBindService(): IShizukuPackageService? = bindMutex.withLock {
        val existing = boundService
        if (existing != null && existing.asBinder().isBinderAlive) {
            return existing
        }
        val deferred = CompletableDeferred<IShizukuPackageService?>()
        pendingConnection = deferred
        val requested = runCatching {
            Shizuku.bindUserService(userServiceArgs, serviceConnection)
            true
        }.getOrDefault(false)
        if (!requested) {
            pendingConnection = null
            return null
        }
        val result = withTimeoutOrNull(4500L) { deferred.await() }
        pendingConnection = null
        result
    }

    private fun statusErrorMessage(status: ShizukuStatus): String = when (status) {
        ShizukuStatus.NOT_INSTALLED -> "Shizuku is not installed."
        ShizukuStatus.NOT_RUNNING -> "Shizuku service is stopped. Open Shizuku and start via Wireless Debugging."
        ShizukuStatus.PERMISSION_REQUIRED -> "Shizuku permission is required."
        ShizukuStatus.READY -> ""
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SHIZUKU_PERMISSION_REQUEST_CODE = 4201
    }
}
