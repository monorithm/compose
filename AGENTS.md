# Rules for agents in this repository

- This repository is the Compose Multiplatform library of the Monorithm Experience Language, `dev.monorithm:ui`. It is greenfield. Do not copy files or history from an archived repository.
- Write all text in ASD-STE100 Simplified Technical English.
- The values come from the language: the submodule `design/` at the commit of one tag of `monorithm/design`. Do not type a value of the language into a composable. Read the tokens with `MonoTheme`, for example `MonoTheme.color.accent`.
- Do not edit `ui/src/commonMain/kotlin/dev/monorithm/ui/Tokens.kt`. Change the commit of the submodule `design/`, then emit again (`./gradlew emitTokens`).
- Do not edit the files of `design/`. Change the language source in `monorithm/design`.
- The emitter and the check of the IR are in `build-logic/`, in the Gradle plugin `dev.monorithm.language`.
- Each test of a component is in the class `<Component>Test`. Its name starts with the clause of the contract, for example `keyboard` for the clause `switch/keyboard`.
- A component emits a moment of the language. It never starts a haptic or a sound directly.
- Put each component in one package in `dev.monorithm.ui.components`, for example `dev.monorithm.ui.components.button`.
- The public API is explicit: each public declaration has the modifier `public` (explicit API mode).
- Use the Gradle wrapper. Keep the versions in `gradle/libs.versions.toml`.
- The repository is public. Do not add these items:
  - A private name
  - The ID of a decision or of a private process
  - The name of a canvas board
