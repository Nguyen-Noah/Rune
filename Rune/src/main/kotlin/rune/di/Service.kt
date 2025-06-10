package rune.di

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Service

object ServiceLocator {
    val bag = mutableMapOf<Class<*>, Any>()

    fun <T: Any> register(type: Class<T>, impl: T) { bag[type] = impl }
    inline fun <reified T: Any> get(): T =
        bag[T::class.java] as? T ?: error("Service ${T::class} not registered.")
}