
## Overview
Rune is an application framework that specializes in rendering geometry with a PBR through the rendering pipeline. It abstracts down the implementation details to allow for further platforms to be added down the line.

## Architecture
`Application.kt` acts as the entry point of the project. Projects will inherit from this and create Layers for their applications. The Runestone module is an application for a game engine, and Sandbox is a sandbox of the rendering system. 


## Code style
- Kotlin strictly
- Be as idiomatic as possible
- Keep everything as simple as possible
- NO emojis, minimal comments
- Import alphabetically
- NO ambiguous variable names. Variables should be recognizable at a first glance