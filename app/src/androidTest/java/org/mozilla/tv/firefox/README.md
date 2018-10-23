# UI & Integration tests README

## Robot pattern
Our tests are written using the robot pattern. This has two primary benefits: 1) less brittle code, and 2) more declarative tests.

1) If our UI changes, we can update the robot to match and other tests should remain valid

2) Tests written using robots include no implementation details, only what is being tested

To learn more about the pattern, see:
- https://academy.realm.io/posts/kau-jake-wharton-testing-robots/
- https://medium.com/android-bits/espresso-robot-pattern-in-kotlin-fc820ce250f7
