package com.example.sergeykuchin.argame

import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator
import timber.log.Timber

abstract class MovingNode: Node(), Controls, Animation.AnimationListener {

    private var countOfXMovements: Int = 0
    private var countOfYMovements: Int = 0
    private var countOfZMovements: Int = 0

    /**
     * Multiplier to each movement
     */
    protected var movementRatio: Float = 1f
    protected var movementStep: Float = 0.1f * movementRatio

    protected var animationDuration: Long = 500L

    protected fun addXMovement(): Int = ++countOfXMovements
    protected fun removeXMovement(): Int = --countOfXMovements

    protected fun addYMovement(): Int = ++countOfYMovements
    protected fun removeYMovement(): Int = --countOfYMovements

    protected fun addZMovement(): Int = ++countOfZMovements
    protected fun removeZMovement(): Int = --countOfZMovements

    protected var objectAnimator: ObjectAnimator? = null

    protected fun startMovement(direction: Direction) {

        if (objectAnimator == null) objectAnimator = createObjectAnimator()

        when(direction) {
            Direction.UP -> goUp()
            Direction.DOWN -> goDown()

            Direction.LEFT -> goLeft()
            Direction.FORWARD -> goForward()
            Direction.RIGHT -> goRight()
            Direction.BACKWARD -> goBackward()
        }
    }

    private fun goUp() {
        addYMovement()

        animateModel(worldPositionAxis, movementStep)
        objectAnimator?.setObjectValues(updateVector3(newY = (worldPosition.y + movementStep), oldVector3 = worldPosition))
    }

    protected fun createObjectAnimator(): ObjectAnimator {
        val objectAnimator = ObjectAnimator()
        objectAnimator.setObjectValues(updateVector3(newY = (worldPosition.y + 0.1f), oldVector3 = worldPosition))
        objectAnimator.propertyName = "worldPosition"
        objectAnimator.setEvaluator(Vector3Evaluator())
        objectAnimator.interpolator = AccelerateDecelerateInterpolator()
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

    override fun onAnimationRepeat(p0: Animation?) {
        Timber.d("onAnimationRepeat")
    }

    override fun onAnimationEnd(p0: Animation?) {
        Timber.d("onAnimationEnd")
    }

    override fun onAnimationStart(p0: Animation?) {
        Timber.d("onAnimationStart")
    }
}