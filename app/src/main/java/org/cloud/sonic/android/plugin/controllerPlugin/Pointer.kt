package org.cloud.sonic.android.plugin.controllerPlugin

import android.graphics.Point

data class Pointer(
  /**
   * Pointer id as received from the client.
   */
  val id:Long,
  /**
   * Local pointer id, using the lowest possible values to fill the {@link android.view.MotionEvent.PointerProperties PointerProperties}.
   */
  val localId:Int,
  var point:Point? = null,
  var pressure:Float? = null,
  var up:Boolean = false
)
