package com.nextcloud.client.core

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.IBinder
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LocalConnectionTest {

    lateinit var context: Context
    lateinit var connection: LocalConnection<Service>
    var mockIntent: Intent? = null

    @MockK
    lateinit var componentName: ComponentName

    @MockK
    lateinit var binder: LocalBinder<Service>

    @MockK
    lateinit var mockOnBound: (IBinder) -> Unit

    @MockK
    lateinit var mockOnUnbound: () -> Unit

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        context = mockk()
        connection = object : LocalConnection<Service>(context) {
            override fun createBindIntent(): Intent? {
                return mockIntent
            }

            override fun onBound(binder: IBinder) {
                mockOnBound.invoke(binder)
            }

            override fun onUnbind() {
                mockOnUnbound.invoke()
            }
        }
    }

    @Test
    fun binding_disabled() {
        // GIVEN
        //      no binding intent is provided
        mockIntent = null

        // WHEN
        //      bind requested
        connection.bind()

        // THEN
        //      no binding is performed
        verify(exactly = 0) { context.bindService(any(), any(), any()) }
    }

    @Test
    fun bind_service() {
        // GIVEN
        //      binding intent is provided
        mockIntent = mockk()

        // WHEN
        //      bind requested
        every { context.bindService(mockIntent, any(), any()) } returns true
        connection.bind()

        // THEN
        //      service bound
        verify { context.bindService(mockIntent, any(), any()) }
    }

    @Test
    fun service_connected() {
        // GIVEN
        //      service is not bound

        // WHEN
        //      service is connected
        connection.onServiceConnected(componentName, binder)

        // THEN
        //      onBound callback called with binder instance
        verify { mockOnBound(binder) }
        assertTrue(connection.isConnected)
    }

    @Test
    fun unbind_service() {
        // GIVEN
        //      servic is bound
        connection.onServiceConnected(componentName, binder)

        // WHEN
        //      service is unbound multiple imes
        justRun { context.unbindService(connection) }
        connection.unbind()
        connection.unbind()

        // THEN
        //      service is unbound only when it's bound
        //      later unbind invocations no-ops
        verify(exactly = 1) { mockOnUnbound.invoke() }
        verify(exactly = 1) { context.unbindService(connection) }
        assertFalse(connection.isConnected)
    }

    @Test(expected = IllegalStateException::class)
    fun binder_must_implement_local_binder() {
        // WHEN
        //      service connected using non-compliant binder
        val badBinder: IBinder = mockk()
        connection.onServiceConnected(componentName, badBinder)

        // THEN
        //      throws
    }
}
