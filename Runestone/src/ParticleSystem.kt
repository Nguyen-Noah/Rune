package runestone

import glm_.glm
import glm_.vec2.Vec2
import glm_.vec4.Vec4
import rune.renderer.OrthographicCamera
import rune.renderer.Renderer
import rune.renderer.Renderer2D
import kotlin.random.Random

data class ParticleProps(
    var position: Vec2 = Vec2(0f),
    var velocity: Vec2 = Vec2(0f),
    var velocityVariation: Vec2 = Vec2(0f),

    var colorBegin: Vec4 = Vec4(0f),
    var colorEnd: Vec4 = Vec4(0f),

    var sizeBegin: Float = 0f,
    var sizeEnd: Float = 0f,
    var sizeVariation: Float = 0f,

    var lifeTime: Float = 1f
)
data class Particle(
    var position: Vec2 = Vec2(0f),
    var velocity: Vec2 = Vec2(0f),
    var rotation: Float = 0f,

    var colorBegin: Vec4 = Vec4(0f),
    var colorEnd: Vec4 = Vec4(0f),

    var sizeBegin: Float = 0f,
    var sizeEnd: Float = 0f,

    var lifeTime: Float = 1f,
    var lifeRemaining: Float = 0f,

    var alive: Boolean = false
)

class ParticleSystem {
    private val poolSize = 1000
    val particles: MutableList<Particle> = MutableList(poolSize) { Particle() }
    var poolIndex = poolSize - 1

    fun onUpdate(dt: Float) {
        // update
        for (particle in particles) {
            if (!particle.alive)
                continue

            if (particle.lifeRemaining <= 0f) {
                particle.alive = false
                continue
            }

            particle.lifeRemaining -= dt
            particle.position = particle.position + particle.velocity * dt
            particle.rotation += 0.01f * dt
        }
    }

    fun onRender(camera: OrthographicCamera) {
        Renderer2D.beginScene(camera)
        for (particle in particles) {
            if (!particle.alive)
                continue

            val life = particle.lifeRemaining / particle.lifeTime
            val color: Vec4 = glm.mix(particle.colorEnd, particle.colorBegin, life)

            val size: Float = glm.mix(particle.sizeEnd, particle.sizeBegin, life)

            Renderer2D.drawRotatedQuad(particle.position, Vec2(size), particle.rotation, color)
        }

        Renderer2D.endScene()
    }

    fun emit(props: ParticleProps) {
        val particle: Particle = particles[poolIndex]
        particle.alive = true
        particle.position = props.position
        particle.rotation = Random.nextFloat() * 2f * glm.PIf

        // velocity
        particle.velocity = props.velocity
        particle.velocity = props.velocity + Vec2(
            props.velocityVariation.x * (Random.nextFloat() - 0.5f),
            props.velocityVariation.y * (Random.nextFloat() - 0.5f)
        )


        // color
        particle.colorBegin = props.colorBegin
        particle.colorEnd = props.colorEnd

        // lifetime
        particle.lifeTime = props.lifeTime
        particle.lifeRemaining = props.lifeTime
        particle.sizeBegin = props.sizeBegin + props.sizeVariation * (Random.nextFloat() - 0.5f)
        particle.sizeEnd = props.sizeEnd

        poolIndex = (poolIndex - 1 + particles.size) % particles.size
    }
}