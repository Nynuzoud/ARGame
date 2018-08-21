package com.example.sergeykuchin.argame.views

import android.os.Bundle
import android.view.View
import com.example.sergeykuchin.argame.R
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import timber.log.Timber

class MyArFragment : ArFragment() {

    private var thawkRenderable: ModelRenderable? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ModelRenderable.builder()
                .setSource(context, R.raw.thunderhawk)
                .build()
                .thenAccept {
                    thawkRenderable = it
                }
                .exceptionally {
                    Timber.d(it)
                    return@exceptionally null
                }

        setOnTapArPlaneListener { hitResult, _, _ ->

            val scene = arSceneView.scene

            val forward = scene.camera.forward
            val worldPosition = scene.camera.worldPosition
            val position = Vector3.add(forward, worldPosition)

            val direction = Vector3.subtract(worldPosition, position)
            direction.y = position.y

            val anchor = arSceneView.session.createAnchor(arSceneView.arFrame.camera.pose.compose(Pose.makeTranslation(0f, 0f, -1.5f)).extractTranslation())
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(scene)

            val tHawkTransformableNode = TransformableNode(transformationSystem)
            tHawkTransformableNode.setParent(anchorNode)
            tHawkTransformableNode.worldPosition = position
            tHawkTransformableNode.setLookDirection(direction)
            tHawkTransformableNode.renderable = thawkRenderable
            tHawkTransformableNode.setLookDirection(Vector3.back(), anchorNode.back)
            tHawkTransformableNode.select()
        }
    }
}