/*
 *  sonic-android-apk  Help your Android device to do more.
 *  Copyright (C) 2022 SonicCloudOrg
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.cloud.sonic.android.lib.socketmanager.utils

import android.os.Looper
import kotlinx.coroutines.*
import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

// 保存 CoroutineScope
private var scopeRef: AtomicReference<Any> = AtomicReference()

// 自定义的 CoroutineScope
val appGlobalScope: CoroutineScope
    get() {
        while (true) {
            val existing = scopeRef.get() as CoroutineScope?
            if (existing != null) {
                return existing
            }
            val newScope = SafeCoroutineScope(Dispatchers.Main.immediate)
            if (scopeRef.compareAndSet(null, newScope)) {
                return newScope
            }
        }
    }

// 不会崩溃的 CoroutineScope
private class SafeCoroutineScope(context: CoroutineContext) : CoroutineScope, Closeable {
    override val coroutineContext: CoroutineContext =
        SupervisorJob() + context + UncaughtCoroutineExceptionHandler()

    override fun close() {
        coroutineContext.cancelChildren()
    }
}

// 自定义 CoroutineExceptionHandler
private class UncaughtCoroutineExceptionHandler : CoroutineExceptionHandler,
    AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineContext {
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        // 处理异常
    }
}


val isOnMainThread: Boolean
    get() = Looper.myLooper() == Looper.getMainLooper()

fun runOnMainThread(block: () -> Unit) {
    if (isOnMainThread) block.invoke()
    else appGlobalScope.launch(Dispatchers.Main) { block.invoke() }
}
