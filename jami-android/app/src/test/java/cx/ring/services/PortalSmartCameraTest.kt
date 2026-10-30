package cx.ring.services

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.facebook.portal.smartcamera.client.base.api.common.ModeSpec
import com.facebook.portal.smartcamera.client.base.api.common.base.Subscriber
import com.facebook.portal.smartcamera.client.base.api.common.concurrent.Subscription
import com.facebook.portal.smartcamera.client.base.api.control.ControlConnection
import com.facebook.portal.smartcamera.client.base.api.control.SmartCameraControlConnectionFactory
import com.facebook.portal.smartcamera.client.base.common.concurrent.SettableAsyncResult
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import java.util.concurrent.Executor

class PortalSmartCameraTest {
    private lateinit var logs: MockedStatic<Log>

    @Before
    fun silenceAndroidLogging() {
        logs = mockStatic(Log::class.java)
    }

    @After
    fun restoreLogging() {
        logs.close()
    }

    @Test
    fun enablesAutomaticFramingOnlyWhenControlBecomesAvailable() {
        val f = Fixture(initialAvailability = false)
        f.controller.start()
        f.connected.set(f.connection)
        verify(f.connection, never()).requestControls()

        f.updates.onUpdate(true)

        val mode = ArgumentCaptor.forClass(ModeSpec::class.java)
        verify(f.session).setMode(mode.capture())
        assertTrue(mode.value is ModeSpec.DefaultAuto)
        f.controller.close()
    }

    @Test
    fun releasesAllResourcesOnceAndIgnoresAvailabilityAfterStop() {
        val f = Fixture()
        f.controller.start()
        f.controller.start()
        f.connected.set(f.connection)

        f.controller.close()
        f.controller.close()
        f.updates.onUpdate(true)

        assertTrue(f.subscriptionClosed)
        verify(f.factory).connect()
        verify(f.connection).requestControls()
        verify(f.session).close()
        verify(f.connection).close()
    }

    @Test
    fun closesConnectionArrivingAfterCaptureHasStopped() {
        val f = Fixture()
        f.controller.start()
        f.controller.close()

        f.connected.set(f.connection)

        verify(f.connection).close()
        verify(f.connection, never()).subscribeControlAvailability()
        verifyNoInteractions(f.session)
    }

    @Test
    fun ignoresAvailabilityAlreadyQueuedWhenCaptureStops() {
        val callbacks = ArrayDeque<Runnable>()
        val f = Fixture(executor = Executor { callbacks.addLast(it) })
        f.controller.start()
        f.connected.set(f.connection)
        callbacks.removeFirst().run()

        f.controller.close()
        while (callbacks.isNotEmpty()) callbacks.removeFirst().run()

        verify(f.connection, never()).requestControls()
        verify(f.connection).close()
    }

    @Test
    fun toleratesPermissionDenialWhileConnecting() {
        val f = Fixture()
        f.controller.start()

        f.connected.setException(SecurityException("permission denied"))

        f.controller.close()
        verifyNoInteractions(f.connection, f.session)
    }

    @Test
    fun toleratesASynchronousConnectionFailure() {
        val f = Fixture()
        `when`(f.factory.connect()).thenThrow(SecurityException("permission denied"))

        f.controller.start()
        f.controller.close()

        verifyNoInteractions(f.connection, f.session)
    }

    @Test
    fun skipsDevicesWithoutThePortalService() {
        val context = mock(Context::class.java)
        val packages = mock(PackageManager::class.java)
        `when`(context.packageManager).thenReturn(packages)
        `when`(packages.getPackageInfo("com.facebook.portal.aiservice", 0))
            .thenThrow(mock(PackageManager.NameNotFoundException::class.java))

        assertNull(PortalSmartCamera.create(context, Executor { it.run() }))
    }

    @Test
    fun releasesControlWhenSettingTheModeFails() {
        val f = Fixture()
        doThrow(SecurityException("control denied")).`when`(f.session).setMode(any())
        f.controller.start()

        f.connected.set(f.connection)

        assertTrue(f.subscriptionClosed)
        verify(f.session).close()
        verify(f.connection).close()
    }

    @Test
    fun reacquiresControlWithoutLettingAnOldSessionClearTheNewOne() {
        val f = Fixture()
        val nextSession = mock(ControlConnection.ControlSession::class.java)
        `when`(nextSession.closeFuture).thenReturn(SettableAsyncResult.create<Void>())
        `when`(f.connection.requestControls()).thenReturn(f.session, nextSession)
        f.controller.start()
        f.connected.set(f.connection)

        f.updates.onUpdate(false)
        f.updates.onUpdate(true)
        f.sessionClosed.set(null)
        f.updates.onUpdate(true)

        verify(f.session).close()
        verify(f.connection, times(2)).requestControls()
        verify(nextSession).setMode(any())
        f.controller.close()
        verify(nextSession).close()
    }

    @Test
    fun releasesTheSessionWhenTheServiceDisconnects() {
        val f = Fixture()
        f.controller.start()
        f.connected.set(f.connection)

        f.connectionClosed.set(null)

        assertTrue(f.subscriptionClosed)
        verify(f.session).close()
        verify(f.connection).close()
    }

    @Test
    fun aFailedCloseDoesNotPreventReleasingTheConnection() {
        val f = Fixture()
        doThrow(IllegalStateException("service disappeared")).`when`(f.session).close()
        f.controller.start()
        f.connected.set(f.connection)

        f.controller.close()

        assertTrue(f.subscriptionClosed)
        verify(f.connection).close()
    }

    private class Fixture(
        initialAvailability: Boolean = true,
        executor: Executor = Executor { it.run() }
    ) {
        val factory = mock(SmartCameraControlConnectionFactory::class.java)
        val connection = mock(ControlConnection::class.java)
        val session = mock(ControlConnection.ControlSession::class.java)
        val connected = SettableAsyncResult.create<ControlConnection>()
        val connectionClosed = SettableAsyncResult.create<Void>()
        val sessionClosed = SettableAsyncResult.create<Void>()
        lateinit var updates: Subscriber<Boolean>
        var subscriptionClosed = false
        @Suppress("UNCHECKED_CAST")
        private val availability = mock(Subscription::class.java) as Subscription<Boolean>
        val controller = PortalSmartCamera(factory, executor)

        init {
            doAnswer { invocation ->
                val subscriber = invocation.getArgument<Subscriber<Boolean>>(0)
                val delivery = invocation.getArgument<Executor>(1)
                updates = Subscriber { value -> delivery.execute { subscriber.onUpdate(value) } }
                if (invocation.getArgument<Boolean>(2)) updates.onUpdate(initialAvailability)
                initialAvailability
            }.`when`(availability).addSubscriber(any(), any(), anyBoolean())
            doAnswer { subscriptionClosed = true; null }.`when`(availability).close()
            `when`(factory.connect()).thenReturn(connected)
            `when`(connection.subscribeControlAvailability()).thenReturn(availability)
            `when`(connection.requestControls()).thenReturn(session)
            `when`(connection.closeFuture).thenReturn(connectionClosed)
            `when`(session.closeFuture).thenReturn(sessionClosed)
        }
    }
}
