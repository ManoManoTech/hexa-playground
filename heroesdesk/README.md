# Heroes Desk domain - Kotlin Arrow implementation

# Long-lasting applications pillars implementations

Below describes how the long-lasting applications pillars described in the [repo README.md](../README.md) are implemented.

## The need for clear business logic: Hexagonal architecture & Domain Driven Design

To avoid that, one can use Domain Driven Design with the Hexagonal Architecture.

Domain Driven Design is all about making the business logic explicit, in one place in the code. More about
it
in [Domain‐Driven Design Reference (PDF)](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf).

Hexagonal architecture is a technical pattern, making sure the business logic is independent of persistence and
presentation. This [2008's article from Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/) is the
best intro to the topic.

Regarding the ports, if sticking strictly to the hexagonal naming, there are 3 so far:

```
org.hexastacks.heroesdesk.kotlin.HeroesDesk
org.hexastacks.heroesdesk.kotlin.ports.UserRepository
org.hexastacks.heroesdesk.kotlin.ports.MissionRepository
```

The HeroesDesk one is the interface exposed to consumers of this domain, the primary one as says Alistair Cockburn
in his [initial article on Hexagonal Architecture](https://alistair.cockburn.us/hexagonal-architecture/).

The 2 others are ones to implement for the previous one to work, secondary ports then.

The primary one is the one describing the module behavior, the business domain. As such it's the first thing to read
when looking it this module: it's thus at the root of the code.
The ports for third parties are only for the ones willing to implement new adapters, a smaller public. As such, they're
shown in the ports package, where adapters' developers are likely to look at.

## Constant change!

So far, the best way i found to get there is proper test coverage, of high quality code quality, hence easy to maintain
and change.

To get there, this repo shows an approach named *Hexagonal testing*: test cases are written against the Hexagon
interface, ie against the business logic.

Then implementations and glue code make sure these tests are run against:

- the model implementation and fake adapters of hexagon ports: easing initial iterations and then allowing for quick
  explorations, while defining early tests without being linked to an implementation,
- the model and each implementation(s), making sure to test implementation properly & in an uniform way,
    - on the way if some tests are lacking, then they're added to the test cases, and then run against all
      implementations,
- finally, implementations of the test cases can be written against its interfaces: public API as well as user
  interfaces, like web pages for examples.

Hence, test cases and data are written once, then run against all implementations, making sure they all behave the same
while costing little to maintain.

To be able to reuse the domain test, the corresponding code must be easy to depend on in any adapter or implementation: as such this code is in the 'heroesdesk-test' module.
This allows to easily depend on it and implement what's needed for reuse.

This has the drawback of moving 'org.hexastacks.heroesdesk.kotlin.HeroesDesk' tests into the `heroesdesk-test` module.

## Factor errors out of existence

This is achieved through:

- strong typed language: a string given for an integer doesn't compile,
- having null safety, meaning the language distinguishes between nullable and non-nullable types. Thus, a null can't be
  provided for a non-nullable type,

As such, the code is using Kotlin, a strong typed language with null safety.

But factor errors out of existence also means making error handling first class code citizen, through the type system, rather than through exceptions out of the
type system. Hence the use of [Kotlin Arrow typed errors](https://arrow-kt.io/learn/typed-errors/working-with-typed-errors/).

It also leans heavily on [Kotlin Arrow Smart constructor pattern](https://arrow-kt.io/learn/typed-errors/validation/#smart-constructors) to make sure domain objects are always in a valid state.