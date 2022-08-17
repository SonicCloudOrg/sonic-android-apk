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
