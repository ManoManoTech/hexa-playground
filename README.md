# Hexa playground

This repo aims at showcasing hexagonal architecture, hexagonal testing and providing a quick start.

Beware though:

- stack is updated & maintained on a best effort basis. Mind versions & dates if reusing,
- for sure, it's likely plainly wrong: we're open to all feedbacks and suggestions.

# Build

First install a Java Development Kit (JDK) version 8 or higher, for example
from [Eclipse Temurin](https://adoptium.net/temurin/releases/?package=jdk&version=21)

Then build with:

```
./gradlew test 
```

This compiles and runs tests on all sub modules.

Test results are then available per module in `build/reports/tests/test/index.html`, for
example `heroes/build/reports/tests/test/index.html`.

# On the difficulty of building long-lasting applications

Often, software development's literature is all about technical details or how quick & easy it is to build applications.

In reality, creating a long-lasting application is a complex endeavor, with more failures than successes.

The present repo aims at helping application development being right. 

For sure, all that follow is needed when dealing with significant & evolving business logic, seen as a long term
investment. Not for "fire & forget" applications.

## Long-lasting applications pillars

Below is a list of key points to have in mind when doing long-lasting application development.

Then each module has a README.md detailing how it's implementing this mantra.

### The need for clear business logic

Often, business logic is scattered across the codebase, at all levels, mixed with technicalities.

For sure, it makes the business logic hard to get. Even worse: it makes the code hard to evolve.

### Constant change!

Whatever the difficulty of building the first version, new ones will come, and the faster they can be build the better.

How to achieve this?

First, is having a clear business logic, decoupled from technicalities, as explained above.

Then, it's the ability to change in confidence, knowing what broke and what didn't, to get the whole working again.

### Factor errors out of existence

A piece of code is obviously better when free of bug.

It's even better when most of the bugs can't happen by design.

# Functional requirements & tech constraints

When doing a tech showcase, which business model to use is often tricky.

A made up application, called HeroesDesk, is the functional target. It's a task tracker for heroes: familiar to
developers, yet allowing some complexity and with a clear mission statement.

Read more about the requirements in [Functional requirement](FUNCTIONAL_REQUIREMENTS.md).

# Implementation

An attempt at applying the above constraints can be seen in the below submodules.

Each module has its own README.md, detailing how it's tackling the constraints.

# License

See [License](LICENSE).

# Code of conduct

See [Code of conduct](CODE_OF_CONDUCT.md).

# Contributing

See [Contributing](CONTRIBUTING.md).