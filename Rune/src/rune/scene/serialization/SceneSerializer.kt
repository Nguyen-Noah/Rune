package rune.scene.serialization

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.quillraven.fleks.Entity
import com.github.quillraven.fleks.World
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import rune.components.*
import rune.core.Logger
import rune.core.UUID
import rune.scene.ProjectionType
import rune.scene.Scene
import java.io.File

private val componentModule = SerializersModule {
    polymorphic(RuneComponent::class) {
        subclass(RuneTagComponent::class,               RuneTagComponent.serializer())
        subclass(RuneTransformComponent::class,         RuneTransformComponent.serializer())
        subclass(RuneCameraComponent::class,            RuneCameraComponent.serializer())
        subclass(RuneSpriteRendererComponent::class,    RuneSpriteRendererComponent.serializer())
    }
}

private val yaml = Yaml(configuration = YamlConfiguration(
    encodeDefaults = false,
    polymorphismStyle = PolymorphismStyle.Tag,
))

class SceneSerializer(private val scene: Scene) {
    fun serialize(path: String) {
        val entities = mutableListOf<RuneEntity>()

        val family = scene.world.family { all(TagComponent) }
        family.forEach { entity ->
            entities.add(scene.world.toDto(entity))
        }

        val dto = RuneScene(
            Scene = "Untitled",
            Entities = entities
        )

        val file = File(path)
        file.parentFile?.mkdirs()   // create the directory if it doesn't exist

        file.writeText(yaml.encodeToString(RuneScene.serializer(), dto))
    }
    fun serializeRuntime(filepath: String) {

    }

    fun deserialize(filepath: String): Boolean {
        val dto = yaml.decodeFromString(RuneScene.serializer(), File(filepath).readText())

        with(scene.world) {
            dto.Entities.forEach { ent ->
                val id = UUID(ent.Entity)
                val tagComp = ent.Components.filterIsInstance<RuneTagComponent>().firstOrNull()
                val name = tagComp?.Tag ?: "Unnamed Entity"

                val entity = scene.createEntityWithUUID(id, name)
                ent.Components.forEach { comp ->
                    entity.configure {
                        when (comp) {
                            is RuneTransformComponent   -> {
                                it[TransformComponent].translation = comp.Translation
                                it[TransformComponent].rotation = comp.Rotation
                                it[TransformComponent].scale = comp.Scale
                            }
                            is RuneCameraComponent -> {
                                it += CameraComponent()
                                val cameraComponent = it[CameraComponent]
                                val camProps = comp.Camera
                                with (cameraComponent.camera) {
                                    projectionType   = ProjectionType.fromInt(camProps.ProjectionType)
                                    perspectiveFOV   = camProps.PerspectiveFOV
                                    perspectiveNear  = camProps.PerspectiveNear
                                    perspectiveFar   = camProps.PerspectiveFar
                                    orthographicSize = camProps.OrthographicSize
                                    orthographicNear = camProps.OrthographicNear
                                    orthographicFar  = camProps.OrthographicFar
                                }
                                cameraComponent.primary = comp.Primary
                                cameraComponent.fixedAspectRatio = comp.FixedAspectRatio
                            }
                            is RuneSpriteRendererComponent -> it += SpriteRendererComponent(comp.Color)

                            // ADD COMPONENTS HERE ------------------------------------------

                            else -> { Logger.warn("Tried to add component $comp, but was not implemented.") }
                        }
                    }
                }
            }
        }

        Logger.info("Loaded scene: ${filepath.split("\\").last()}")
        return true
    }
    fun deserializeRuntime(filepath: String): Boolean {
        return true
    }

    private fun World.toDto(entity: Entity): RuneEntity {
        val components = mutableListOf<RuneComponent>()

        // tag
        components += RuneTagComponent(entity[TagComponent].tag)

        // transform
        entity[TransformComponent].let { t ->
            components += RuneTransformComponent(t.translation, t.rotation, t.scale)
        }

        // optional components
        if (entity.has(CameraComponent)) {
            val cc = entity[CameraComponent]
            val cam = cc.camera
            components += RuneCameraComponent(
                Camera = RuneCamera(
                    ProjectionType   = cam.projectionType.ordinal,
                    PerspectiveFOV   = cam.perspectiveFOV,
                    PerspectiveNear  = cam.perspectiveNear,
                    PerspectiveFar   = cam.perspectiveFar,
                    OrthographicSize = cam.orthographicSize,
                    OrthographicNear = cam.orthographicNear,
                    OrthographicFar  = cam.orthographicFar
                ),
                Primary             = cc.primary,
                FixedAspectRatio    = cc.fixedAspectRatio
            )
        }

        if (entity.has(SpriteRendererComponent)) {
            components += RuneSpriteRendererComponent(
                entity[SpriteRendererComponent].color
            )
        }



        return RuneEntity(
            entity[IDComponent].id.value,
            components
        )
    }
}


@Serializable
sealed interface RuneComponent

@Serializable
private data class RuneScene(
    val Scene: String,
    val Entities: List<RuneEntity>
) : RuneComponent

@Serializable
private data class RuneEntity(
    val Entity: Long,
    val Components: List<RuneComponent> = emptyList()
) : RuneComponent

@Serializable
private data class RuneTagComponent(val Tag: String) : RuneComponent

@Serializable
private data class RuneTransformComponent(
    @Serializable(with = Vec3AsList::class) val Translation: Vec3,
    @Serializable(with = Vec3AsList::class) val Rotation: Vec3,
    @Serializable(with = Vec3AsList::class) val Scale: Vec3
) : RuneComponent

@Serializable
private data class RuneCameraComponent(
    val Camera: RuneCamera,
    val Primary: Boolean,
    val FixedAspectRatio: Boolean
) : RuneComponent

@Serializable
private data class RuneCamera(
    val ProjectionType: Int,
    val PerspectiveFOV: Float,
    val PerspectiveNear: Float,
    val PerspectiveFar: Float,
    val OrthographicSize: Float,
    val OrthographicNear: Float,
    val OrthographicFar: Float
)

@Serializable
private data class RuneSpriteRendererComponent(
    @Serializable(with = Vec4AsList::class) val Color: Vec4
) : RuneComponent