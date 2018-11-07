# ADR 3: Refactoring into a Model/View/View-Model (MVVM) architecture

## Context
The initial version of the codebase was built without a specific architecture in mind, and this has led to a few problems:
1. New UX flow proposals were incompatible with previous assumptions of how different views were bound together, and those were fragile and difficult to change.
1. Integrating large pieces of the android-components library caused many regressions, partly because state management is distributed throughout different parts of the UI.
1. Testing app state (especially the BrowserOverlay, or homescreen) was difficult because side effects of actions were spread out throughout the app, rather than being consolidated.
1. A formal architecture was needed to provide clarity for where to change/add code as the number of people working with the codebase grows.

Formal architectures address these problems by explicitly decoupling the state and UI. The common ones are MVC/MVP, MVVM, MVI.

## Decision
We decided to use [Google's MVVM with Architecture Components](https://developer.android.com/jetpack/docs/guide) because in addition to satisfying the other requirements of separating state and views, it also allowed for incremental change, which was important because switching to a new architecture would extend past a single sprint, and there was the possibility that we’d need to respond to critical issues during the refactor. It’s also a familiar and well-documented architecture, and integrates well with Android’s `ViewModel` and `LiveData`.

We also considered MVI, which would make testing easier because state is immutable, but MVI didn’t allow for incremental change, and has a steeper learning curve. We also decided against MVP because it doesn’t enforce very clear separations between model and presenter, which often leads to bloated presenters, and could complicate Android lifecycle management.

### Model
- Different Repositories hold state for components of the app old state
- Logic to handle actions that change the model

### ViewModel
- Observes changes in Repositories and updates views
- Calls actions exposed by Model from UI interactions

### Fragments
- Connect VM and Model during `Fragment#onViewCreated` by setting VM observers on the Model

Status: Accepted

## Consequences
- We will refactor the MainActivity into the MVVM architecture
- Future code additions to MainActivity should be made using the MVVM architecture
- MainActivity is tightly coupled to MVVM: if we choose to abandon the MVVM architecture without refactoring existing code, it could potentially be more confusing
- Our MVVM code is expected to be more maintainable because there is a greater clarity and testability with the separation that MVVM provides
- Our MVVM code will take marginally longer to write, at least in the short term as we learn the best way to express the pattern
- We merge architectural changes into master early in sprints to give QA enough time to test for regressions

