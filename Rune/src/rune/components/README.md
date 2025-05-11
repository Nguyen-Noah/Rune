**Procedure For Adding Components**
1. Create a new component using the `flekscomponent` Live Template
2. Register the component in `scene/serialization/FleksJson.kt`
3. Add component to `World.copyComponentsToEntity` in `Scene.kt`