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
package org.cloud.sonic.android.plugin.controllerPlugin

import android.view.MotionEvent.PointerCoords
import android.view.MotionEvent.PointerProperties
import java.util.concurrent.Executors

class ControllerManager {
    private val DEFAULT_DEVICE_ID = 0
    private val EXECUTOR = Executors.newSingleThreadScheduledExecutor()

  private val lastTouchDown: Long = 0
  private val pointersState: PointersState = PointersState()
  private val pointerProperties = arrayOfNulls<PointerProperties>(PointersState.MAX_POINTERS)
  private val pointerCoords = arrayOfNulls<PointerCoords>(PointersState.MAX_POINTERS)


}
