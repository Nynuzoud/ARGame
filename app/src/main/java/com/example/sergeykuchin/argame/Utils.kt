package com.example.sergeykuchin.argame

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import timber.log.Timber

class Utils {

    companion object {

        private const val MIN_OPENGL_VERSION = 3.0

        /**
         * Creates an ARCore session. This checks for the CAMERA permission, and if granted, checks the
         * state of the ARCore installation. If there is a problem an exception is thrown. Care must be
         * taken to update the installRequested flag as needed to avoid an infinite checking loop. It
         * should be set to true if null is returned from this method, and called again when the
         * application is resumed.
         *
         * @param activity - the activity currently active.
         * @param installRequested - the indicator for ARCore that when checking the state of ARCore, if
         * an installation was already requested. This is true if this method previously returned
         * null. and the camera permission has been granted.
         */
        fun createArSession(activity: Activity, installRequested: Boolean): Session? {
            var session: Session? = null
            // if we have the camera permission, create the session
            if (hasCameraPermission(activity)) {
                when (ArCoreApk.getInstance().requestInstall(activity, !installRequested)) {
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> return null
                    else -> {}
                }
                session = Session(activity)
                // IMPORTANT!!!  ArSceneView requires the `LATEST_CAMERA_IMAGE` non-blocking update mode.
                val config = Config(session)
                config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                session.configure(config)
            }
            return session
        }

        /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
        fun requestCameraPermission(activity: Activity, requestCode: Int) {
            ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.CAMERA), requestCode)
        }

        /** Check to see we have the necessary permissions for this app.  */
        fun hasCameraPermission(activity: Activity): Boolean {
            return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        }

        /** Check to see if we need to show the rationale for this permission.  */
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.CAMERA)
        }

        /** Launch Application Setting to grant permission.  */
        fun launchPermissionSettings(activity: Activity) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.fromParts("package", activity.packageName, null)
            activity.startActivity(intent)
        }

        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Timber.e("Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show()
                activity.finish()
                return false
            }
            return true
        }
    }
}