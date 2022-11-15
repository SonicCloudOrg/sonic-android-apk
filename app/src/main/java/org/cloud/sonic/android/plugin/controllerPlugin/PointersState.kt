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

class PointersState {
    companion object {
        const val MAX_POINTERS = 10
    }

    private val pointers:MutableList<Pointer> = ArrayList()

    private fun indexOf(id:Long) : Int {
      for (i in pointers.indices) {
        val pointer = pointers[i]
        if (pointer.id == id) {
          return i
        }
      }
      return -1
    }

    private fun isLocalIdAvailable(localId:Int) : Boolean {
      for (i in pointers.indices) {
        val pointer = pointers[i]
        if (pointer.localId == localId) {
          return false
        }
      }
      return true
    }

  private fun nextUnusedLocalId(): Int {
    for (localId in 0 until MAX_POINTERS) {
      if (isLocalIdAvailable(localId)) {
        return localId
      }
    }
    return -1
  }

  operator fun get(index: Int): Pointer? {
    return pointers[index]
  }

  fun getPointerIndex(id: Long): Int {
    val index = indexOf(id)
    if (index != -1) {
      // already exists, return it
      return index
    }
    if (pointers.size >= MAX_POINTERS) {
      // it's full
      return -1
    }
    // id 0 is reserved for mouse events
    val localId = nextUnusedLocalId()
    if (localId == -1) {
      throw AssertionError("pointers.size() < maxFingers implies that a local id is available")
    }
    val pointer = Pointer(id, localId)
    pointers.add(pointer)
    // return the index of the pointer
    return pointers.size - 1
  }
}
