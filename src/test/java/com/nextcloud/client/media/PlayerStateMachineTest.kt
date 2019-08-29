/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.media

import com.nextcloud.client.media.PlayerStateMachine.Event
import com.nextcloud.client.media.PlayerStateMachine.State
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(Suite::class)
@Suite.SuiteClasses(
    PlayerStateMachineTest.Constructor::class,
    PlayerStateMachineTest.EventHandling::class,
    PlayerStateMachineTest.Stopped::class,
    PlayerStateMachineTest.Downloading::class,
    PlayerStateMachineTest.Preparing::class,
    PlayerStateMachineTest.AwaitFocus::class,
    PlayerStateMachineTest.Focused::class,
    PlayerStateMachineTest.Ducked::class,
    PlayerStateMachineTest.Paused::class
)
internal class PlayerStateMachineTest {

    abstract class Base {
        @Mock
        protected lateinit var delegate: PlayerStateMachine.Delegate
        protected lateinit var fsm: PlayerStateMachine

        fun setUp(initialState: State) {
            MockitoAnnotations.initMocks(this)
            fsm = PlayerStateMachine(initialState, delegate)
        }
    }

    class Constructor {

        private val delegate: PlayerStateMachine.Delegate = mock()

        @Test
        fun `default state is stopped`() {
            val fsm = PlayerStateMachine(delegate)
            assertEquals(State.STOPPED, fsm.state)
        }

        @Test
        fun `inital state can be set`() {
            val fsm = PlayerStateMachine(State.PREPARING, delegate)
            assertEquals(State.PREPARING, fsm.state)
        }
    }

    class EventHandling : Base() {

        @Before
        fun setUp() {
            super.setUp(State.STOPPED)
        }

        @Test
        fun `can post multiple events from callback`() {
            whenever(delegate.isDownloaded).thenReturn(false)
            whenever(delegate.isAutoplayEnabled).thenReturn(false)
            whenever(delegate.hasEnqueuedFile).thenReturn(true)
            whenever(delegate.onStartDownloading()).thenAnswer {
                fsm.post(Event.DOWNLOADED)
                fsm.post(Event.PREPARED)
            }

            // WHEN
            //      an event is posted from a state machine callback
            fsm.post(Event.PLAY) // posts error() in callback

            // THEN
            //      enqueued events is handled triggering transitions
            assertEquals(State.PAUSED, fsm.state)
            verify(delegate).onStartRunning()
            verify(delegate).onStartDownloading()
            verify(delegate).onPrepare()
            verify(delegate).onPausePlayback()
        }

        @Test
        fun `unhandled events are ignored`() {
            // GIVEN
            //      state machine is in STOPPED state
            //      PAUSE event is not handled in this staet

            // WHEN
            //      state machine receives unhandled PAUSE event
            fsm.post(Event.PAUSE)

            // THEN
            //      event is ignored
            //      exception is not thrown
        }
    }

    class Stopped : Base() {

        @Before
        fun setUp() {
            super.setUp(State.STOPPED)
        }

        @Test
        fun `initiall state is stopped`() {
            assertEquals(State.STOPPED, fsm.state)
        }

        @Test
        fun `playing requires enqueued file`() {
            // GIVEN
            //      no file is enqueued
            whenever(delegate.hasEnqueuedFile).thenReturn(false)

            // WHEN
            //      play is triggered
            fsm.post(Event.PLAY)

            // THEN
            //      remains in stopped state
            assertEquals(State.STOPPED, fsm.state)
        }

        @Test
        fun `playing remote media triggers downloading`() {
            // GIVEN
            //      file is enqueued
            //      media is not downloaded
            whenever(delegate.hasEnqueuedFile).thenReturn(true)
            whenever(delegate.isDownloaded).thenReturn(false)

            // WHEN
            //      play is requested
            fsm.post(Event.PLAY)

            // THEN
            //      enqueued file is loaded
            //      media stream download starts
            assertEquals(State.DOWNLOADING, fsm.state)
            verify(delegate).onStartRunning()
            verify(delegate).onStartDownloading()
        }

        @Test
        fun `playing local media triggers player preparation`() {
            // GIVEN
            //      file is enqueued
            //      media is downloaded
            whenever(delegate.hasEnqueuedFile).thenReturn(true)
            whenever(delegate.isDownloaded).thenReturn(true)

            // WHEN
            //      play is requested
            fsm.post(Event.PLAY)

            // THEN
            //      player preparation starts
            assertEquals(State.PREPARING, fsm.state)
            verify(delegate).onPrepare()
        }
    }

    class Downloading : Base() {

        // GIVEN
        //      player is downloading stream URL
        @Before
        fun setUp() {
            setUp(State.DOWNLOADING)
        }

        @Test
        fun `stream url download is successfull`() {
            // WHEN
            //      stream url downloaded
            fsm.post(Event.DOWNLOADED)

            // THEN
            //      player is preparing
            assertEquals(State.PREPARING, fsm.state)
            verify(delegate).onPrepare()
        }

        @Test
        fun `stream url download failed`() {
            // WHEN
            //      download error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onError()
        }

        @Test
        fun `player stopped`() {
            // WHEN
            //      download error
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun `player error`() {
            // WHEN
            //      player error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            //      error handler is called
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onError()
        }
    }

    class Preparing : Base() {

        @Before
        fun setUp() {
            setUp(State.PREPARING)
        }

        @Test
        fun `start in autoplay mode`() {
            // GIVEN
            //      media player is preparing
            //      autoplay is enabled
            whenever(delegate.isAutoplayEnabled).thenReturn(true)

            // WHEN
            //      media player is ready
            fsm.post(Event.PREPARED)

            // THEN
            //      start playing
            //      request audio focus
            //      awaiting focus
            assertEquals(State.AWAIT_FOCUS, fsm.state)
            verify(delegate).onRequestFocus()
        }

        @Test
        fun `start in paused mode`() {
            // GIVEN
            //      media player is preparing
            //      autoplay is disabled
            whenever(delegate.isAutoplayEnabled).thenReturn(false)

            // WHEN
            //      media player is ready
            fsm.post(Event.PREPARED)

            // THEN
            //      media player is not started
            assertEquals(State.PAUSED, fsm.state)
            verify(delegate, never()).onStartPlayback()
        }

        @Test
        fun `player is stopped during preparation`() {
            // GIVEN
            //      media player is preparing
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun `error during preparation`() {
            // GIVEN
            //      media player is preparing
            // WHEN
            //      download error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            //      error callback is invoked
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onError()
        }
    }

    class AwaitFocus : Base() {

        @Before
        fun setUp() {
            setUp(State.AWAIT_FOCUS)
        }

        @Test
        fun pause() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      media player is paused
            fsm.post(Event.PAUSE)

            // THEN
            //      media player enters paused state
            //      focus is released
            assertEquals(State.PAUSED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onReleaseFocus()
                verify(delegate).onPausePlayback()
            }
        }

        @Test
        fun `audio focus denied`() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      audio focus was denied
            fsm.post(Event.FOCUS_LOST)

            // THEN
            //      media player enters paused state
            assertEquals(State.PAUSED, fsm.state)
            verify(delegate).onPausePlayback()
        }

        @Test
        fun `audio focus granted`() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      audio focus was granted
            fsm.post(Event.FOCUS_GAIN)

            // THEN
            //      media player enters focused state
            //      playback is started
            assertEquals(State.FOCUSED, fsm.state)
            verify(delegate).onStartPlayback()
        }

        @Test
        fun stop() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            //      focus is released
            assertEquals(State.STOPPED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onReleaseFocus()
                verify(delegate).onStopped()
            }
        }

        @Test
        fun error() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            //      focus is released
            assertEquals(State.STOPPED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onReleaseFocus()
                verify(delegate).onError()
            }
        }
    }

    class Focused : Base() {

        @Before
        fun setUp() {
            setUp(State.FOCUSED)
        }

        @Test
        fun pause() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      media player is paused
            fsm.post(Event.PAUSE)

            // THEN
            //      media player enters paused state
            //      focus is released
            assertEquals(State.PAUSED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onReleaseFocus()
                verify(delegate).onPausePlayback()
            }
        }

        @Test
        fun `lost focus`() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      media player lost audio focus
            fsm.post(Event.FOCUS_LOST)

            // THEN
            //      media player enters paused state
            //      focus is released
            assertEquals(State.PAUSED, fsm.state)
            verify(delegate).onPausePlayback()
        }

        @Test
        fun `audio focus duck`() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      media player focus duck is requested
            fsm.post(Event.FOCUS_DUCK)

            // THEN
            //      media player ducks
            assertEquals(State.DUCKED, fsm.state)
            verify(delegate).onAudioDuck(eq(true))
        }

        @Test
        fun stop() {
            // GIVEN
            //      media player is awaiting focus
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            //      focus is released
            assertEquals(State.STOPPED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onReleaseFocus()
                verify(delegate).onStopped()
            }
        }

        @Test
        fun error() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            //      focus is released
            //      error is signaled
            assertEquals(State.STOPPED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onReleaseFocus()
                verify(delegate).onError()
            }
        }
    }

    class Ducked : Base() {

        @Before
        fun setUp() {
            setUp(State.DUCKED)
        }

        @Test
        fun pause() {
            // GIVEN
            //      media player is playing
            //      audio focus is ducked
            // WHEN
            //      media player is paused
            fsm.post(Event.PAUSE)

            // THEN
            //      audio focus duck is disabled
            //      focus is released
            //      playback is paused
            assertEquals(State.PAUSED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onAudioDuck(eq(false))
                verify(delegate).onReleaseFocus()
                verify(delegate).onPausePlayback()
            }
        }

        @Test
        fun `lost focus`() {
            // GIVEN
            //      media player is playing
            //      audio focus is ducked
            // WHEN
            //      media player is looses focus
            fsm.post(Event.FOCUS_LOST)

            // THEN
            //      audio focus duck is disabled
            //      focus is released
            //      playback is paused
            assertEquals(State.PAUSED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onAudioDuck(eq(false))
                verify(delegate).onReleaseFocus()
                verify(delegate).onPausePlayback()
            }
            // WHEN
            //      media player is paused
            fsm.post(Event.PAUSE)

            // THEN
            //      audio focus duck is disabled
            //      focus is released
            //      playback is paused
            assertEquals(State.PAUSED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onAudioDuck(eq(false))
                verify(delegate).onReleaseFocus()
                verify(delegate).onPausePlayback()
            }
        }

        @Test
        fun `audio focus is re-gained`() {
            // GIVEN
            //      media player is playing
            //      audio focus is ducked
            // WHEN
            //      media player focus duck is requested
            fsm.post(Event.FOCUS_GAIN)

            // THEN
            //      media player is focused
            //      audio focus duck is disabled
            //      playback is not restarted
            assertEquals(State.FOCUSED, fsm.state)
            verify(delegate).onAudioDuck(eq(false))
            verify(delegate, never()).onStartPlayback()
        }

        @Test
        fun stop() {
            // GIVEN
            //      media player is playing
            //      audio focus is ducked
            // WHEN
            //      media player is stopped
            fsm.post(Event.STOP)

            // THEN
            //      audio focus duck is disabled
            //      focus is released
            //      playback is stopped
            assertEquals(State.STOPPED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onAudioDuck(eq(false))
                verify(delegate).onReleaseFocus()
                verify(delegate).onStopped()
            }
        }

        @Test
        fun error() {
            // GIVEN
            //      media player is playing
            //      audio focus is ducked
            // WHEN
            //      error
            fsm.post(Event.ERROR)

            // THEN
            //      audio focus duck is disabled
            //      focus is released
            //      playback is stopped
            //      error is signaled
            assertEquals(State.STOPPED, fsm.state)
            inOrder(delegate).run {
                verify(delegate).onAudioDuck(eq(false))
                verify(delegate).onReleaseFocus()
                verify(delegate).onError()
            }
        }
    }

    class Paused : Base() {

        @Before
        fun setUp() {
            setUp(State.PAUSED)
        }

        @Test
        fun pause() {
            // GIVEN
            //      media player is paused
            // WHEN
            //      media player is resumed
            fsm.post(Event.PLAY)

            // THEN
            //      media player enters playing state
            //      audio focus is requsted
            assertEquals(State.AWAIT_FOCUS, fsm.state)
            verify(delegate).onRequestFocus()
        }

        @Test
        fun stop() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      stopped
            fsm.post(Event.STOP)

            // THEN
            //      player is stopped
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onStopped()
        }

        @Test
        fun error() {
            // GIVEN
            //      media player is playing
            // WHEN
            //      error
            fsm.post(Event.ERROR)

            // THEN
            //      player is stopped
            //      error callback is invoked
            assertEquals(State.STOPPED, fsm.state)
            verify(delegate).onError()
        }
    }
}
