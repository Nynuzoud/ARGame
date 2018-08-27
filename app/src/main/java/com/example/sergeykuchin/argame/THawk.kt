package com.example.sergeykuchin.argame

import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable

class THawk: MovingNode() {

    fun create(anchorNode: AnchorNode, position: Vector3, direction: Vector3, thawkRenderable: ModelRenderable): Node {
        setParent(anchorNode)
        worldPosition = position
        setLookDirection(direction)
        renderable = thawkRenderable
        setLookDirection(Vector3.back(), anchorNode.back)
        return this
    }

}