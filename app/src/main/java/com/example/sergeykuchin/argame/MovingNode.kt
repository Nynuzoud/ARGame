package com.example.sergeykuchin.argame

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Handler
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import timber.log.Timber

abstract class MovingNode: Node(), Controls, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private var xRight = false
    private var xLeft = false
    private var yUp = false
    private var yDown = false
    private var zForward = false
    private var zBackward = false

    /**
     * Multiplier to each movement
     */
    protected var movementRatio: Float = 1f
    protected var movementStep: Float = 0.1f * movementRatio

    protected var animationDuration: Long = 200L

    protected var objectAnimator: ObjectAnimator? = null

    protected var movementStatus: MovementStatus = MovementStatus.STOPPED
    private var isFirstMovement = true

    private var movementThread: Thread? = null
    private var movementThreadHandler: Handler? = null
    private val START_ANIMATION_CODE = 324

    override fun goUp() {
        if (!yDown) {
            yUp = true
        }
    }

    override fun releaseUp() {
        yUp = false
    }

    override fun goDown() {
        if (!yUp) {
            yDown = true
        }
    }

    override fun releaseDown() {
        yDown = false
    }


    override fun goRight() {
        if (!xLeft) {
            xRight = true
        }
    }

    override fun releaseRight() {
        xRight = false
    }

    override fun goLeft() {
        if (!xRight) {
            xLeft = true
        }
    }

    override fun releaseLeft() {
        xLeft = false
    }



    override fun goForward() {
        if (!zBackward) {
            zForward = true
        }
    }

    override fun releaseForward() {
        zForward = false
    }

    override fun goBackward() {
        if (!zForward) {
            zBackward = true
        }
    }

    override fun releaseBackward() {
        zBackward = false
    }

    fun startMovementHandler() {
        if (objectAnimator == null) objectAnimator = createObjectAnimator()
        if (movementThreadHandler == null) movementThreadHandler = Handler { message ->
            when (message.what) {
                START_ANIMATION_CODE -> {
                    Timber.d("TEST")
                }
            }

            return@Handler true
        }

        movementThread = Thread(Runnable {

            while (movementThread?.isAlive == true) {

                if (!isEveryButtonReleased()) {
                    if (xRight) {
                        objectAnimator?.setObjectValues(updateVector3(newX = (worldPosition.x + movementStep), oldVector3 = worldPosition))
                    }
                    if (xLeft) {
                        objectAnimator?.setObjectValues(updateVector3(newX = (worldPosition.x - movementStep), oldVector3 = worldPosition))
                    }
                    if (yUp) {
                        objectAnimator?.setObjectValues(updateVector3(newY = (worldPosition.y + movementStep), oldVector3 = worldPosition))
                    }
                    if (yDown) {
                        objectAnimator?.setObjectValues(updateVector3(newY = (worldPosition.y - movementStep), oldVector3 = worldPosition))
                    }
                    if (zForward) {
                        objectAnimator?.setObjectValues(updateVector3(newZ = (worldPosition.z + movementStep), oldVector3 = worldPosition))
                    }
                    if (zBackward) {
                        objectAnimator?.setObjectValues(updateVector3(newZ = (worldPosition.z - movementStep), oldVector3 = worldPosition))
                    }

                    if (isFirstMovement) {
                        movementStatus = MovementStatus.STARTING
                        objectAnimator?.interpolator = AccelerateInterpolator()

                        movementThreadHandler?.sendEmptyMessage(START_ANIMATION_CODE)

                        isFirstMovement = false
                    }

                }
            }
        })
        movementThread?.start()
    }

    protected fun createObjectAnimator(): ObjectAnimator {
        val objectAnimator = ObjectAnimator()
        objectAnimator.propertyName = "worldPosition"
        objectAnimator.addUpdateListener(this)
        objectAnimator.addListener(this)
        objectAnimator.setEvaluator(Vector3Evaluator())
        //objectAnimator.interpolator = LinearInterpolator()
        objectAnimator.setAutoCancel(true)

        objectAnimator.target = this
        objectAnimator.duration = animationDuration

        return objectAnimator
    }

    protected fun updateVector3(newX: Float? = null, newY: Float? = null, newZ: Float? = null, oldVector3: Vector3): Vector3 {
        newX?.let { oldVector3.x = newX }
        newY?.let { oldVector3.y = newY }
        newZ?.let { oldVector3.z = newZ }

        return oldVector3
    }

    private fun isEveryButtonReleased(): Boolean = !xRight && !xLeft && !yUp && !yDown && !zForward && !zBackward

    override fun onAnimationUpdate(valueAnimator: ValueAnimator?) {
        valueAnimator?.currentPlayTime
        valueAnimator?.animatedValue as Float
    }

    override fun onAnimationRepeat(p0: Animator?) {

    }

    override fun onAnimationEnd(p0: Animator?) {
        when (movementStatus) {
            MovementStatus.STARTING -> {
                movementStatus = MovementStatus.MOVING
                objectAnimator?.interpolator = LinearInterpolator()
                objectAnimator?.start()
            }
            MovementStatus.MOVING -> {
                if (isEveryButtonReleased()) {
                    movementStatus = MovementStatus.ENDING
                    objectAnimator?.interpolator = DecelerateInterpolator()
                    objectAnimator?.start()
                }
            }
            MovementStatus.ENDING -> {
                movementStatus = MovementStatus.STOPPED
                isFirstMovement = true
            }
            else -> {}
        }
    }

    override fun onAnimationCancel(p0: Animator?) {

    }

    override fun onAnimationStart(p0: Animator?) {


    }
}