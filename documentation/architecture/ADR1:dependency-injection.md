## Context
##### What is DI?

In DI (dependency injection), client code does not create its own dependencies.
```kotlin
// Without DI, Repository creates its own PocketEndpoint
class Repository {
    val endpoint = PocketEndpoint()
}

class Parent {
    val repo = Repository()
}
```
```kotlin
// With DI, endpoint is injected (passed) through the constructor...
class Repository(val endpoint: Endpoint) {
}

class Parent {
    val repo = Repository(PocketEndpoint())
}

// ... or through a field
class Repository {
    var endpoint: Endpoint
}

class Parent {
    val repo = Repository()

    init {
        repo.endpoint = PocketEndpoint()
    }
}
```
See https://en.wikipedia.org/wiki/Dependency_injection

##### Why inject?

Injection makes testing simpler. When testing, we don't want our repos to make normal network calls (these make tests flaky, as calls can fail or return unexpected data). So we provide fake endpoints that return the data we need. This is much simpler with DI.

It also makes it easier to follow the adage "program to an interface, not an implementation." Client code (in this case, the Repository) can define the interface upon which they depend, and then never even import the actual implementation. This helps us to separate concerns.

##### Dependency injection vs service locators

We use a service locator pattern. This is similar to, but distinct from dependency injection.

In dependency injection, client code declares that they use an object (either in their constructor, or a field), and it is provided from outside. The client code has no idea how this occurs. In the service locator pattern, client code has a hard dependency upon a service locator, which is used to fulfill dependencies. The above example can be rewritten as a service locator as follows:
```kotlin
class Repository(serviceLocator: ServiceLocator) {
    val endpoint = serviceLocator.endpoint()
}
```

This has some downsides. We now have a direct dependency upon the SL, which will make switching away from the pattern (or specific framework) later very difficult. Testing can also be harder if we use a singleton SL, although this can be mitigated by injecting the SL through constructors. This allows us to provide a mocked SL in tests.

The advantages of SL when compared to DI are that it is very simple, and in practice does not require a framework to employ. Additionally, refactoring legacy code to follow DI is very difficult, whereas refactoring into SL is often very easy.

##### Why not use a DI framework?

Dagger 2 is the most commonly used DI framework on Android. It is very fast at runtime for a DI framework, and is extremely feature rich. Unfortunately, imo, it suffers from feature bloat, and is extremely difficult to learn. Dagger 2 uses a lot of 'black magic' to get things done. The actual logic in Dagger 2 is relatively straightforward and understandable, but learning how it works often involves piecing together generated code, as the documentation largely covers 'how' to do things, not 'why.' Dagger 2 also has a negative impact on build times, which can eventually grow to be substantial.

Koin and Kodein are SL frameworks written in Kotlin.  These provide nice features, but each have their own particular syntax and usage patterns. Given how simple our dependency tree is (and is expected to be in the future), we decided to write our SL by hand.

##### Why not maintain singletons in the Application?

While this is an option, it would not be an alternative to the service locator pattern. It would only turn the Application itself into a service locator. This would be messy, as Application has other responsibilities that should not be mixed with the provisioning of objects.

## Status
Accepted

## Consequences
- We will use a service locator pattern in our app
- This will make mocking resources during tests simpler
- Our app will be tightly coupled to the SL pattern, and switching away from it will be made difficult
