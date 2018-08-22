package com.example.sergeykuchin.argame

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_control_buttons.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val CAM_PERMISSION = 123

    private var thawkRenderable: ModelRenderable? = null
    private var hasPlacedTHawk = false
    private var hasFinishedLoading = false
    private var loadingMessageSnackbar: Snackbar? = null
    private var installRequested: Boolean = false
    private var tHawk: THawk? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Utils.checkIsSupportedDeviceOrFinish(this)) {
            // Not a supported device.
            return
        }

        buildTHawkModel()
        setUpSceneTouchListener()
        setUpSceneUpdateListener()
        Utils.requestCameraPermission(this, CAM_PERMISSION)

        handleControlEvents()
    }

    private fun buildTHawkModel() {
        ModelRenderable.builder()
                .setSource(this, R.raw.thunderhawk)
                .build()
                .thenAccept {
                    thawkRenderable = it
                    hasFinishedLoading = true
                }
                .exceptionally {
                    Timber.d(it)
                    return@exceptionally null
                }
    }

    private fun setUpSceneTouchListener() {
        ar_scene_view
                .scene
                .setOnTouchListener { hitTestResult: HitTestResult, event: MotionEvent ->
                    if (!hasPlacedTHawk) {
                        onSingleTap()
                    }
                    false
                }
    }

    private fun setUpSceneUpdateListener() {
        ar_scene_view
                .scene
                .addOnUpdateListener {
                    if (loadingMessageSnackbar == null) {
                        return@addOnUpdateListener
                    }

                    val frame = ar_scene_view.arFrame ?: return@addOnUpdateListener

                    if (frame.camera.trackingState != TrackingState.TRACKING) {
                        return@addOnUpdateListener
                    }

                    for (plane in frame.getUpdatedTrackables(Plane::class.java)) {
                        if (plane.trackingState == TrackingState.TRACKING) {
                            hideLoadingMessage()
                            showControls()
                        }
                    }
                }
    }

    private fun onSingleTap() {
        //check if scene has loaded
        if (!hasFinishedLoading) {
            return
        }

        //try to place tHawk if it isn't placed yet
        if (!hasPlacedTHawk && tryToPlaceTHawk()) {
            hasPlacedTHawk = true
        }
    }

    private fun tryToPlaceTHawk(): Boolean {
        val scene = ar_scene_view.scene

        val forward = scene.camera.forward
        val worldPosition = scene.camera.worldPosition
        val position = Vector3.add(forward, worldPosition)

        val direction = Vector3.subtract(worldPosition, position)
        direction.y = position.y

        val anchor = ar_scene_view.session.createAnchor(ar_scene_view.arFrame.camera.pose.compose(Pose.makeTranslation(0f, 0f, -1.5f)).extractTranslation())
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(scene)

        if (thawkRenderable != null) {
            tHawk = THawk()
            tHawk?.create(anchorNode, position, direction, thawkRenderable!!)
        }
        return tHawk != null
    }

    private fun handleControlEvents() {
        up.setOnClickListener { tHawk?.goUp() }
        down.setOnClickListener { tHawk?.goDown() }

        left.setOnClickListener { tHawk?.goLeft() }
        forward.setOnClickListener { tHawk?.goForward() }
        right.setOnClickListener { tHawk?.goRight() }
        backward.setOnClickListener { tHawk?.goBackward() }
    }

    private fun showLoadingMessage() {
        if (loadingMessageSnackbar != null && loadingMessageSnackbar?.isShownOrQueued == true) {
            return
        }

        loadingMessageSnackbar = Snackbar.make(
                findViewById<ViewGroup>(android.R.id.content),
                R.string.plane_finding,
                Snackbar.LENGTH_INDEFINITE)
        loadingMessageSnackbar?.show()
    }

    private fun hideLoadingMessage() {
        if (loadingMessageSnackbar == null) {
            return
        }

        loadingMessageSnackbar?.dismiss()
        loadingMessageSnackbar = null
    }

    private fun showControls() {
        control_buttons.visibility = View.VISIBLE
    }

    private fun hideControls() {
        control_buttons.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (ar_scene_view == null) {
            return
        }

        if (ar_scene_view.session == null) {
            // If the session wasn't created yet, don't resume rendering.
            // This can happen if ARCore needs to be updated or permissions are not granted yet.
            try {
                val session = Utils.createArSession(this, installRequested)
                if (session == null) {
                    installRequested = Utils.hasCameraPermission(this)
                    return
                } else {
                    ar_scene_view.setupSession(session)
                }
            } catch (e: UnavailableException) {
                Timber.e(e)
            }

        }

        try {
            ar_scene_view.resume()
        } catch (ex: CameraNotAvailableException) {
            Timber.e(ex)
            finish()
            return
        }

        if (ar_scene_view.session != null) {
            showLoadingMessage()
            hideControls()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (ar_scene_view != null) {
            ar_scene_view.pause()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!Utils.hasCameraPermission(this)) {
            if (!Utils.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                Utils.launchPermissionSettings(this)
            } else {
                Toast.makeText(
                        this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                        .show()
            }
            finish()
        }
    }
}
