package com.example.sergeykuchin.argame

import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import timber.log.Timber

class THawk: Node(), Controls {

    fun create(anchorNode: AnchorNode, position: Vector3, direction: Vector3, thawkRenderable: ModelRenderable): Node {
        setParent(anchorNode)
        worldPosition = position
        setLookDirection(direction)
        renderable = thawkRenderable
        setLookDirection(Vector3.back(), anchorNode.back)
        return this
    }

    override fun goUp() {
        Timber.d("goUp")
        worldPosition = updateVector3(newY = (worldPosition.y + 0.01f), oldVector3 = worldPosition)
    }

    override fun goDown() {
        Timber.d("goDown")
    }

    override fun goLeft() {
        Timber.d("goLeft")
    }

    override fun goForward() {
        Timber.d("goForward")
    }

    override fun goRight() {
        Timber.d("goRight")
    }

    override fun goBackward() {
        Timber.d("goBackward")
    }

    private fun updateVector3(newX: Float? = null, newY: Float? = null, newZ: Float? = null, oldVector3: Vector3): Vector3 {
        newX?.let { oldVector3.x = newX }
        newY?.let { oldVector3.y = newY }
        newZ?.let { oldVector3.z = newZ }

        return oldVector3
    }
}