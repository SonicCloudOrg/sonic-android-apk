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
