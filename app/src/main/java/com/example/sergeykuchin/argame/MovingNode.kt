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

    @Volatile
    private var xRight = false

    @Volatile
    private var xLeft = false

    @Volatile
    private var yUp = false

    @Volatile
    private var yDown = false

    @Volatile
    private var zForward = false

    @Volatile
    private var zBackward = false

    private val releasesQueue = PriorityQueue<Directions>()

    /**
     * Multiplier to each movement
     */
    protected var movementRatio: Float = 1f
    protected var movementStep: Float = 0.05f * movementRatio

    protected var startingAnimationDuration: Long = 500L
    protected var movingAnimationDuration: Long = 200L
    protected var endingAnimationDuration: Long = 500L

    @Volatile
    private var objectAnimator: ObjectAnimator? = null

    @Volatile
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
        releasesQueue.add(Directions.Y_UP)
    }

    override fun goDown() {
        if (!yUp) {
            yDown = true
        }
    }

    override fun releaseDown() {
        releasesQueue.add(Directions.Y_DOWN)
    }


    override fun goRight() {
        if (!xLeft) {
            xRight = true
        }
    }

    override fun releaseRight() {
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
        releasesQueue.add(Directions.Z_FORWARD)
    }

    override fun goBackward() {
        if (!zForward) {
            zBackward = true
        }
    }

    override fun releaseBackward() {
        releasesQueue.add(Directions.Z_BACKWARD)
    }

    protected fun releaseButtons() {
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

                    updateObjectAnimator(MovementStatus.STARTING)

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

    private fun createObjectAnimator(): ObjectAnimator {
        val objectAnimator = ObjectAnimator()
        objectAnimator.propertyName = "worldPosition"
        objectAnimator.addUpdateListener(this)
        objectAnimator.addListener(this)
        objectAnimator.setAutoCancel(true)

        objectAnimator.target = this

        return objectAnimator
    }

    private fun updateVector3(newX: Float? = null, newY: Float? = null, newZ: Float? = null, oldVector3: Vector3): Vector3 {
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

    private fun updateObjectAnimator(movementStatus: MovementStatus) {
        when (movementStatus) {
            MovementStatus.STOPPED -> {
                this.movementStatus = MovementStatus.STOPPED
            }
            MovementStatus.STARTING -> {
                this.movementStatus = MovementStatus.STARTING
                objectAnimator?.duration = startingAnimationDuration
                objectAnimator?.interpolator = AccelerateInterpolator()
            }
            MovementStatus.MOVING -> {
                this.movementStatus = MovementStatus.MOVING
                objectAnimator?.duration = movingAnimationDuration
                objectAnimator?.interpolator = LinearInterpolator()
            }
            MovementStatus.ENDING -> {
                this.movementStatus = MovementStatus.ENDING
                objectAnimator?.duration = endingAnimationDuration
                objectAnimator?.interpolator = DecelerateInterpolator()
            }
        }
    }

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
                    updateObjectAnimator(MovementStatus.ENDING)
                } else {
                    updateObjectAnimator(MovementStatus.MOVING)
                }
                objectAnimator?.start()
            }
            MovementStatus.MOVING -> {
                if (isEveryButtonReleased()) {
                    updateObjectAnimator(MovementStatus.ENDING)
                }
                objectAnimator?.start()
            }
            MovementStatus.ENDING -> {
                updateObjectAnimator(MovementStatus.STOPPED)
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