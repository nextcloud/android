/*
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
package com.nextcloud.client.logger

import android.os.Handler
import android.util.Log
import com.nextcloud.client.core.Clock
import java.util.Date
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Suppress("TooManyFunctions")
internal class LoggerImpl(
    private val clock: Clock,
    private val handler: FileLogHandler,
    private val mainThreadHandler: Handler,
    queueCapacity: Int
) : Logger, LogsRepository {

    data class Load(val onResult: (List<LogEntry>, Long) -> Unit)
    class Delete

    private val looper = ThreadLoop()
    private val eventQueue: BlockingQueue<Any> = LinkedBlockingQueue(queueCapacity)

    private val processedEvents = mutableListOf<Any>()
    private val otherEvents = mutableListOf<Any>()
    private val missedLogs = AtomicBoolean()
    private val missedLogsCount = AtomicLong()

    override val lostEntries: Boolean
        get() {
            return missedLogs.get()
        }

    fun start() {
        looper.start(this::eventLoop)
    }

    override fun v(tag: String, message: String) {
        Log.v(tag, message)
        enqueue(Level.VERBOSE, tag, message)
    }

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
        enqueue(Level.DEBUG, tag, message)
    }

    override fun d(tag: String, message: String, t: Throwable) {
        Log.d(tag, message)
        enqueue(Level.DEBUG, tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
        enqueue(Level.INFO, tag, message)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
        enqueue(Level.WARNING, tag, message)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
        enqueue(Level.ERROR, tag, message)
    }

    override fun e(tag: String, message: String, t: Throwable) {
        Log.e(tag, message)
        enqueue(Level.ERROR, tag, message)
    }

    override fun load(onLoaded: (entries: List<LogEntry>, totalLogSize: Long) -> Unit) {
        eventQueue.put(Load(onLoaded))
    }

    override fun deleteAll() {
        eventQueue.put(Delete())
    }

    private fun enqueue(level: Level, tag: String, message: String) {
        try {
            val entry = LogEntry(timestamp = clock.currentDate, level = level, tag = tag, message = message)
            val enqueued = eventQueue.offer(entry, 1, TimeUnit.SECONDS)
            if (!enqueued) {
                missedLogs.set(true)
                missedLogsCount.incrementAndGet()
            }
        } catch (ex: InterruptedException) {
            // since interrupted flag is consumed now, we need to re-set the flag so
            // the caller can continue handling the thread interruption in it's own way
            Thread.currentThread().interrupt()
        }
    }

    private fun eventLoop() {
        try {
            processedEvents.clear()
            otherEvents.clear()

            processedEvents.add(eventQueue.take())
            eventQueue.drainTo(processedEvents)

            // process all writes in bulk - this is most frequest use case and we can
            // assume handler must be opened 99.999% of time; anything that is not a log
            // write should be deferred
            handler.open()
            for (event in processedEvents) {
                if (event is LogEntry) {
                    handler.write(event.toString() + "\n")
                } else {
                    otherEvents.add(event)
                }
            }
            handler.close()

            // Those events are very sporadic and we don't have to be clever here
            for (event in otherEvents) {
                when (event) {
                    is Load -> {
                        val loaded = handler.loadLogFiles()
                        val entries = loaded.lines.mapNotNull { LogEntry.parse(it) }
                        mainThreadHandler.post {
                            event.onResult(entries, loaded.logSize)
                        }
                    }
                    is Delete -> handler.deleteAll()
                }
            }

            checkAndLogLostMessages()
        } catch (ex: InterruptedException) {
            handler.close()
            throw ex
        }
    }

    private fun checkAndLogLostMessages() {
        val lastMissedLogsCount = missedLogsCount.getAndSet(0)
        if (lastMissedLogsCount > 0) {
            handler.open()
            val warning = LogEntry(
                timestamp = Date(),
                level = Level.WARNING,
                tag = "Logger",
                message = "Logger queue overflow. Approx $lastMissedLogsCount entries lost. You write too much."
            ).toString()
            handler.write(warning)
            handler.close()
        }
    }
}
