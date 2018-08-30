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
import java.util.*

abstract class MovingNode: Node(), Controls, ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private enum class Directions {
        X_RIGHT,
        X_LEFT,
        Y_UP,
        Y_DOWN,
        Z_FORWARD,
        Z_BACKWARD
    }

    private var xRight = false
    private var xLeft = false
    private var yUp = false
    private var yDown = false
    private var zForward = false
    private var zBackward = false

    private val releasesQueue = PriorityQueue<Directions>()

    /**
     * Multiplier to each movement
     */
    protected var movementRatio: Float = 1f
    protected var movementStep: Float = 0.1f * movementRatio

    protected var animationDuration: Long = 200L

    @Volatile
    protected var objectAnimator: ObjectAnimator? = null

    protected var movementStatus: MovementStatus = MovementStatus.STOPPED

    private var movementThread: Thread? = null
    private var movementThreadHandler: Handler? = null
    private val START_ANIMATION_CODE = 324

    override fun goUp() {
        if (!yDown) {
            yUp = true
        }
    }

    override fun releaseUp() {
        //yUp = false
        releasesQueue.add(Directions.Y_UP)
    }

    override fun goDown() {
        if (!yUp) {
            yDown = true
        }
    }

    override fun releaseDown() {
        //yDown = false
        releasesQueue.add(Directions.Y_DOWN)
    }


    override fun goRight() {
        if (!xLeft) {
            xRight = true
        }
    }

    override fun releaseRight() {
        //xRight = false
        releasesQueue.add(Directions.X_RIGHT)
    }

    override fun goLeft() {
        if (!xRight) {
            xLeft = true
        }
    }

    override fun releaseLeft() {
        //xLeft = false
        releasesQueue.add(Directions.X_LEFT)
    }



    override fun goForward() {
        if (!zBackward) {
            zForward = true
        }
    }

    override fun releaseForward() {
        //zForward = false
        releasesQueue.add(Directions.Z_FORWARD)
    }

    override fun goBackward() {
        if (!zForward) {
            zBackward = true
        }
    }

    override fun releaseBackward() {
        //zBackward = false
        releasesQueue.add(Directions.Z_BACKWARD)
    }

    private fun releaseButtons() {
        releasesQueue.forEach { direction ->
            when (direction) {
                Directions.Y_UP -> yUp = false
                Directions.Y_DOWN -> yDown = false
                Directions.X_RIGHT -> xRight = false
                Directions.X_LEFT -> xLeft = false
                Directions.Z_FORWARD -> zForward = false
                Directions.Z_BACKWARD -> zBackward = false
                else -> {}
            }
        }
    }

    fun startMovementHandler() {
        if (objectAnimator == null) objectAnimator = createObjectAnimator()

        if (movementThreadHandler == null) movementThreadHandler = Handler { message ->
            when (message.what) {
                START_ANIMATION_CODE -> {
                    objectAnimator?.start()
                }
            }

            return@Handler true
        }

        movementThread = Thread {

            while (movementThread?.isAlive == true) {

                if (!isEveryButtonReleased() && movementStatus == MovementStatus.STOPPED) {
                    setNewPosition()

                    objectAnimator?.setEvaluator(Vector3Evaluator())
                    movementStatus = MovementStatus.STARTING
                    objectAnimator?.interpolator = AccelerateInterpolator()

                    movementThreadHandler?.sendEmptyMessage(START_ANIMATION_CODE)

                }
            }
        }
        movementThread?.isDaemon = true
        movementThread?.start()
    }

    private fun setNewPosition() {
        if (xRight || releasesQueue.contains(Directions.X_RIGHT)) {
            objectAnimator?.setObjectValues(worldPosition, updateVector3(newX = (worldPosition.x + movementStep), oldVector3 = worldPosition))
        }
        if (xLeft || releasesQueue.contains(Directions.X_LEFT)) {
            objectAnimator?.setObjectValues(worldPosition, updateVector3(newX = (worldPosition.x - movementStep), oldVector3 = worldPosition))
        }
        if (yUp || releasesQueue.contains(Directions.Y_UP)) {
            objectAnimator?.setObjectValues(worldPosition, updateVector3(newY = (worldPosition.y + movementStep), oldVector3 = worldPosition))
        }
        if (yDown || releasesQueue.contains(Directions.Y_DOWN)) {
            objectAnimator?.setObjectValues(worldPosition, updateVector3(newY = (worldPosition.y - movementStep), oldVector3 = worldPosition))
        }
        if (zForward || releasesQueue.contains(Directions.Z_FORWARD)) {
            objectAnimator?.setObjectValues(worldPosition, updateVector3(newZ = (worldPosition.z - movementStep), oldVector3 = worldPosition))
        }
        if (zBackward || releasesQueue.contains(Directions.Z_BACKWARD)) {
            objectAnimator?.setObjectValues(worldPosition, updateVector3(newZ = (worldPosition.z + movementStep), oldVector3 = worldPosition))
        }
    }

    protected fun createObjectAnimator(): ObjectAnimator {
        val objectAnimator = ObjectAnimator()
        objectAnimator.propertyName = "worldPosition"
        objectAnimator.addUpdateListener(this)
        objectAnimator.addListener(this)
        objectAnimator.setAutoCancel(true)

        objectAnimator.target = this
        objectAnimator.duration = animationDuration

        return objectAnimator
    }

    protected fun updateVector3(newX: Float? = null, newY: Float? = null, newZ: Float? = null, oldVector3: Vector3): Vector3 {
        val newVector3 = Vector3()

        newVector3.x = oldVector3.x
        newVector3.y = oldVector3.y
        newVector3.z = oldVector3.z

        newX?.let { newVector3.x = newX }
        newY?.let { newVector3.y = newY }
        newZ?.let { newVector3.z = newZ }

        return newVector3
    }

    private fun isEveryButtonReleased(): Boolean = !xRight && !xLeft && !yUp && !yDown && !zForward && !zBackward

    override fun onAnimationUpdate(valueAnimator: ValueAnimator?) {
        valueAnimator?.currentPlayTime
        valueAnimator?.animatedValue as Vector3
    }

    override fun onAnimationRepeat(p0: Animator?) {

    }

    override fun onAnimationEnd(animator: Animator?) {

        val endValue = (animator as ObjectAnimator).animatedValue as Vector3
        worldPosition = endValue
        setNewPosition()
        when (movementStatus) {
            MovementStatus.STARTING -> {
                if (isEveryButtonReleased()) {
                    movementStatus = MovementStatus.ENDING
                    objectAnimator?.interpolator = DecelerateInterpolator()
                } else {
                    movementStatus = MovementStatus.MOVING
                    objectAnimator?.interpolator = LinearInterpolator()

                }
                objectAnimator?.start()
            }
            MovementStatus.MOVING -> {
                if (isEveryButtonReleased()) {
                    movementStatus = MovementStatus.ENDING
                    objectAnimator?.interpolator = DecelerateInterpolator()
                }
                objectAnimator?.start()
            }
            MovementStatus.ENDING -> {
                movementStatus = MovementStatus.STOPPED
                releasesQueue.clear()
            }
            else -> {}
        }
        releaseButtons()
    }

    override fun onAnimationCancel(p0: Animator?) {

    }

    override fun onAnimationStart(p0: Animator?) {


    }
}