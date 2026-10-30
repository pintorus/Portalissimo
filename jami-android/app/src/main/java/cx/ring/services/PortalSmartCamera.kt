package cx.ring.services

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.facebook.portal.smartcamera.client.base.api.common.ModeSpec
import com.facebook.portal.smartcamera.client.base.api.common.concurrent.Subscription
import com.facebook.portal.smartcamera.client.base.api.control.ControlConnection
import com.facebook.portal.smartcamera.client.base.api.control.SmartCameraControlConnectionFactory
import java.util.concurrent.Executor

// CameraService runs lifecycle calls and SDK callbacks on its video handler.
internal class PortalSmartCamera(
    private val factory: SmartCameraControlConnectionFactory,
    private val executor: Executor
) : AutoCloseable {
    private var started = false
    private var closed = false
    private var connection: ControlConnection? = null
    private var availability: Subscription<Boolean>? = null
    private var session: ControlConnection.ControlSession? = null

    fun start() {
        if (started || closed) return
        started = true
        try {
            factory.connect().addListener({ result ->
                try {
                    if (!result.isSuccessful) {
                        fallback(result.throwable)
                        return@addListener
                    }
                    val connected = requireNotNull(result.value)
                    if (closed) {
                        release(connected)
                        return@addListener
                    }
                    connection = connected
                    availability = connected.subscribeControlAvailability()
                    availability!!.addSubscriber({ available ->
                        if (!closed) updateAvailability(available == true)
                    }, executor, true)
                    connected.closeFuture.addListener({
                        if (!closed) {
                            Log.i(TAG, "Smart Camera disconnected; keeping standard camera")
                            close()
                        }
                    }, executor)
                } catch (e: Exception) {
                    fallback(e)
                }
            }, executor)
        } catch (e: Exception) {
            fallback(e)
        }
    }

    private fun updateAvailability(available: Boolean) {
        if (!available) {
            val previous = session
            session = null
            release(previous)
            Log.i(TAG, "Smart Camera control busy; keeping standard camera")
            return
        }
        if (session != null) return
        try {
            val acquired = requireNotNull(connection?.requestControls())
            session = acquired
            acquired.setMode(ModeSpec.DefaultAuto.create())
            acquired.closeFuture.addListener({
                if (session === acquired) {
                    session = null
                    Log.i(TAG, "Smart Camera control revoked; keeping standard camera")
                }
            }, executor)
            Log.i(TAG, "Smart Camera automatic framing enabled")
        } catch (e: Exception) {
            fallback(e)
        }
    }

    private fun fallback(error: Throwable?) {
        Log.w(TAG, "Smart Camera unavailable; keeping standard camera", error)
        close()
    }

    override fun close() {
        if (closed) return
        closed = true
        val subscription = availability
        val previousSession = session
        val previousConnection = connection
        availability = null
        session = null
        connection = null
        release(subscription)
        release(previousSession)
        release(previousConnection)
    }

    private fun release(resource: AutoCloseable?) {
        try {
            resource?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release Smart Camera resource", e)
        }
    }

    companion object {
        private const val TAG = "PortalSmartCamera"
        private const val SERVICE_PACKAGE = "com.facebook.portal.aiservice"

        fun create(context: Context, executor: Executor): PortalSmartCamera? = try {
            context.packageManager.getPackageInfo(SERVICE_PACKAGE, 0)
            PortalSmartCamera(SmartCameraControlConnectionFactory(context), executor)
        } catch (_: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Smart Camera service not installed; keeping standard camera")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize Smart Camera; keeping standard camera", e)
            null
        }
    }
}
