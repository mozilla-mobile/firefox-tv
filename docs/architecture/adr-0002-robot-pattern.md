# ADR 2: Robot Pattern in UI Tests
## Context
UI tests are notoriously difficult to maintain. A few problems that UI tests have are that they often:
1. Lack a clear architecture
1. Repeat complex logic
1. Are written imperatively

After briefly searching, only one architecture for UI tests comes up: **the Robot pattern.** The Robot pattern separates the "how" and the "what" concerns of a UI test. The tests handle the what - click this button, assert the state, click that button, assert the state - while the robots handle the "how" - how to find the button view, how to click the button, and how to actually assert the state, i.e. the implementation details.

The Robot pattern results in declarative tests like:
```kotlin
navigationOverlay {
    assertCanGoBack()
    goBack()
    assertCanNotGoBack()

}.enterUrlAndEnterToBrowser("https://mozilla.org") {
    assertBodyContent("Welcome to mozilla.org!")
}
```

In the above example, the top-level functions are screen transitions while the inner scope functions are interactions on a given screen.

To learn more about the Robot pattern, see these resources:
- [Brief introduction (missing screen transitions)](https://medium.com/android-bits/espresso-robot-pattern-in-kotlin-fc820ce250f7)
- [Presentation introducing the pattern](https://academy.realm.io/posts/kau-jake-wharton-testing-robots/)
- [Slide deck from presentation](https://jakewharton.com/testing-robots/)

Pros of the Robot pattern:
- Declarative test files
- Discourages repetition of complex "how" logic by centralizing in robots
- Reduces the number of places test code needs to change for UX changes, e.g.:
  - If only one screen changes, only one robot, and perhaps the reliant tests, needs to change
  - Small UX changes (e.g. polish) generally only change the robot code
- Clearly separates which interactions occur on which screens
- Reduced scope when implementing functionality through robot abstractions

Cons of the Robot pattern:
- To write robots and debug test failures, it's a pattern that must be learned
- Relies on Kotlin features so probably won't be readable in Java
- Test failure call stacks are less clear due to nested function calls to support the test DSL
- Robot screen transition implementations use uncommon Kotlin syntax to support the test DSL
- Hard to write generic re-usable code, as required by the robots

Neutral notes on the Robot pattern:
- Doesn't define how to handle interactions that don't fit into a screen (e.g. mutating internal state, clicking hardware remote buttons)

## Decision
We will architect our UI tests using the Robot pattern: there are no obvious alternatives and the pros significantly outweigh the cons when addressing our specific problems.

Status: Accepted

## Consequences
- Tests written with the Robot pattern should be more maintainable but are expected to need more upfront design and brain power
- All new UI tests will be written using the Robot pattern and Kotlin
- All existing UI tests will be refactored to use the Robot pattern (to reduce code duplication) and Kotlin
- Test developers need to learn about the robot pattern in order to debug tests and modify the robots
- The QA teams, in order to maintain these tests, need to familiarize themselves with Kotlin
- The test harness is not coupled to the robot pattern so if we have issues we can easily go back to writing tests without an architecture as before
