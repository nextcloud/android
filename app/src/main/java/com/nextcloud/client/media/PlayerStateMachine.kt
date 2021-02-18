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

import com.github.oxo42.stateless4j.StateMachine
import com.github.oxo42.stateless4j.StateMachineConfig
import com.github.oxo42.stateless4j.delegates.Action
import com.github.oxo42.stateless4j.transitions.Transition
import java.util.ArrayDeque

/*
 * To see visual representation of the state machine, install PlanUml plugin.
 * http://plantuml.com/
 *
 * @startuml
 *
 * note "> - entry action\n< - exit action\n[exp] - transition guard\nfunction() - transition action" as README
 *
 * [*] --> STOPPED
 * STOPPED --> RUNNING: PLAY\n[hasEnqueuedFile]
 * RUNNING --> STOPPED: STOP\nonStop
 * RUNNING --> STOPPED: ERROR\nonError
 * RUNNING: >onStartRunning
 *
 * state RUNNING {
 *      [*] --> DOWNLOADING: [!isDownloaded]
 *      [*] --> PREPARING: [isDownloaded]
 *      DOWNLOADING: >onStartDownloading
 *      DOWNLOADING --> PREPARING: DOWNLOADED
 *
 *      PREPARING: >onPrepare
 *      PREPARING --> PLAYING: PREPARED\n[autoPlay]
 *      PREPARING --> PAUSED: PREPARED\n[!autoPlay]
 *      PLAYING --> PAUSED: PAUSE\nFOCUS_LOST
 *
 *      PAUSED: >onPausePlayback
 *      PAUSED --> PLAYING: PLAY
 *
 *      PLAYING: >onRequestFocus
 *      PLAYING: <onReleaseFocus
 *      state PLAYING {
 *          [*] -r-> AWAIT_FOCUS
 *          AWAIT_FOCUS --> FOCUSED: FOCUS_GAIN\nonStartPlayback()
 *          FOCUSED -l-> DUCKED: FOCUS_DUCK
 *          DUCKED: >onAudioDuck(true)\n<onAudioDuck(false)
 *          DUCKED -r-> FOCUSED: FOCUS_GAIN
 *      }
 * }
 *
 * @enduml
 */
internal class PlayerStateMachine(initialState: State, private val delegate: Delegate) {

    constructor(delegate: Delegate) : this(State.STOPPED, delegate)

    interface Delegate {
        val isDownloaded: Boolean
        val isAutoplayEnabled: Boolean
        val hasEnqueuedFile: Boolean

        fun onStartRunning()
        fun onStartDownloading()
        fun onPrepare()
        fun onStopped()
        fun onError()
        fun onStartPlayback()
        fun onPausePlayback()
        fun onRequestFocus()
        fun onReleaseFocus()
        fun onAudioDuck(enabled: Boolean)
    }

    enum class State {
        STOPPED,
        RUNNING,
        RUNNING_INITIAL,
        DOWNLOADING,
        PREPARING,
        PAUSED,
        PLAYING,
        AWAIT_FOCUS,
        FOCUSED,
        DUCKED
    }

    enum class Event {
        PLAY,
        DOWNLOADED,
        PREPARED,
        STOP,
        PAUSE,
        ERROR,
        FOCUS_LOST,
        FOCUS_GAIN,
        FOCUS_DUCK,
        IMMEDIATE_TRANSITION,
    }

    private var pendingEvents = ArrayDeque<Event>()
    private var isProcessing = false
    private val stateMachine: StateMachine<State, Event>

    /**
     * Immediate state machine state. This attribute provides innermost active state.
     * For checking parent states, use [PlayerStateMachine.isInState].
     */
    val state: State
        get() {
            return stateMachine.state
        }

    init {
        val config = StateMachineConfig<State, Event>()

        config.configure(State.STOPPED)
            .permitIf(Event.PLAY, State.RUNNING_INITIAL) { delegate.hasEnqueuedFile }
            .onEntryFrom(Event.STOP, delegate::onStopped)
            .onEntryFrom(Event.ERROR, delegate::onError)

        config.configure(State.RUNNING)
            .permit(Event.STOP, State.STOPPED)
            .permit(Event.ERROR, State.STOPPED)
            .onEntry(delegate::onStartRunning)

        config.configure(State.RUNNING_INITIAL)
            .substateOf(State.RUNNING)
            .permitIf(Event.IMMEDIATE_TRANSITION, State.DOWNLOADING, { !delegate.isDownloaded })
            .permitIf(Event.IMMEDIATE_TRANSITION, State.PREPARING, { delegate.isDownloaded })
            .onEntry(this::immediateTransition)

        config.configure(State.DOWNLOADING)
            .substateOf(State.RUNNING)
            .permit(Event.DOWNLOADED, State.PREPARING)
            .onEntry(delegate::onStartDownloading)

        config.configure(State.PREPARING)
            .substateOf(State.RUNNING)
            .permitIf(Event.PREPARED, State.AWAIT_FOCUS) { delegate.isAutoplayEnabled }
            .permitIf(Event.PREPARED, State.PAUSED) { !delegate.isAutoplayEnabled }
            .onEntry(delegate::onPrepare)

        config.configure(State.PLAYING)
            .substateOf(State.RUNNING)
            .permit(Event.PAUSE, State.PAUSED)
            .permit(Event.FOCUS_LOST, State.PAUSED)
            .onEntry(delegate::onRequestFocus)
            .onExit(delegate::onReleaseFocus)

        config.configure(State.PAUSED)
            .substateOf(State.RUNNING)
            .permit(Event.PLAY, State.AWAIT_FOCUS)
            .onEntry(delegate::onPausePlayback)

        config.configure(State.AWAIT_FOCUS)
            .substateOf(State.PLAYING)
            .permit(Event.FOCUS_GAIN, State.FOCUSED)

        config.configure(State.FOCUSED)
            .substateOf(State.PLAYING)
            .permit(Event.FOCUS_DUCK, State.DUCKED)
            .onEntry(this::onAudioFocusGain)

        config.configure(State.DUCKED)
            .substateOf(State.PLAYING)
            .permit(Event.FOCUS_GAIN, State.FOCUSED)
            .onEntry(Action { delegate.onAudioDuck(true) })
            .onExit(Action { delegate.onAudioDuck(false) })

        stateMachine = StateMachine(initialState, config)
        stateMachine.onUnhandledTrigger { _, _ -> /* ignore unhandled event */ }
    }

    private fun immediateTransition() {
        stateMachine.fire(Event.IMMEDIATE_TRANSITION)
    }

    private fun onAudioFocusGain(t: Transition<State, Event>) {
        if (t.source == State.AWAIT_FOCUS) {
            delegate.onStartPlayback()
        }
    }

    /**
     * Check if state machine is in a given state.
     * Contrary to [PlayerStateMachine.state] attribute, this method checks for
     * parent states.
     */
    fun isInState(state: State): Boolean {
        return stateMachine.isInState(state)
    }

    /**
     * Post state machine event to internal queue.
     *
     * This design ensures that we're not triggering multiple events
     * from state machines callbacks before the transition is fully
     * completed.
     *
     * Method is re-entrant.
     */
    fun post(event: Event) {
        pendingEvents.addLast(event)
        if (!isProcessing) {
            isProcessing = true
            while (pendingEvents.isNotEmpty()) {
                val processedEvent = pendingEvents.removeFirst()
                stateMachine.fire(processedEvent)
            }
            isProcessing = false
        }
    }
}
