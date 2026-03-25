
## Overview
`Sandbox` is a project that is a Rune application. It will act as a simple window with a framebuffer that has the active scene in it, as well as an ImGui Layer to configure renderer settings. This will act as a sandbox to test newly implemented features in the Rune renderer.

## Architecture
`Sandbox.kt` acts as the entry point of the project, which inherits from the Rune project. The two layers that must be present are the scene layer and an ImGui Layer.

## Common patterns
Reference the `Runestone` module for common paterns.

## Code style
- Kotlin strictly
- Be as idiomatic as possible
- Keep everything as simple as possible
- NO emojis, minimal comments
- Import alphabetically