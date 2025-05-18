package rune.scene

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType
import ktx.box2d.BodyDefinition
import ktx.box2d.body
import ktx.box2d.createWorld

class PhysicsWorld2D(
    private var gravity: Vector2 = Vector2(0f, -9.8f)
) {
    private var physicsWorld: PhysicsWorld = createWorld(gravity)

    var velocityIterations = 6      // how often is it doing calculations
    var positionIterations = 2      // TODO: expose these to the editor

    fun reset() { physicsWorld = createWorld(gravity) }
    fun onStart() = reset()
    fun onUpdate(dt: Float) = physicsWorld.step(dt, velocityIterations, positionIterations)
    fun body(type: BodyType = BodyType.StaticBody, init: BodyDefinition.() -> Unit): Body = physicsWorld.body(type, init)
}
