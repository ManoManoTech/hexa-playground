# Hexa playground

This repo aims at showcasing hexagonal architecture, hexagonal testing and providing a quick start.

Beware though:

- stack is updated & maintained on a best effort basis. Mind versions & dates if reusing,
- for sure, it's likely plainly wrong: i'm open to all feedbacks and suggestions.

# Quick-start/Usage

Build with

```
./gradlew test 
```

# Motivations

## The need for clear business logic

Often, business logic is scattered across the codebase, at all levels, mixed with technicalities.

For sure, it makes the business logic hard to get, but even worse it makes the code hard to evolve.

To avoid that, one can use Domain Driven Design with a hexagonal architecture.

Domain Driven Design is all about making the business logic explicit, in one place in the code. More can be read about
it
in [Domain‐Driven Design Reference (PDF)](https://www.domainlanguage.com/wp-content/uploads/2016/05/DDD_Reference_2015-03.pdf).

Hexagonal architecture is a technical pattern, making sure the business logic is independent of persistence and
presentation. This [2008's article from Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/) is the
best intro to the topic.

For sure, all the above is needed when dealing with significant & evolving business logic, seen as a long term
investment.

## The need for constant change

Whatever the difficulty of building the first version, new ones will come, and the faster they can be build the better.

How to achieve this?

First, is having a clear business logic, decoupled from technicalities, as explained above.

Then, it's the ability to change in confidence, knowing what broke and what didn't, to get the whole working again.

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

## Factor errors out of existence

A piece of code is obviously better when free of bug.

It's even better when most of the bugs can't happen by design.

This is notably the case when:

- using a strong typed language: a string can't be given for an integer,
- having null safety, meaning the language distinguishes between nullable and non nullable types. Thus a null can't be
  given for a non nullable type,
- making error handling first class code citizen, through the type system, rather than through exceptions out of the
  type system.

As such, the code is using Kotlin, a strong typed language with null safety. It also uses [Arrow](https://arrow-kt.io/)
to make error handling a first class citizen.

# Functional requirements & tech constraints: the HeroesDesk app

When doing a tech showcase, which business model to use is often tricky.
The following one, HeroesDesk, was chosen because it's familiar to developers, allows some complexity and has a clear
mission statement.

## HeroesDesk: task tracker for heroes :)

No time tracking, no endless workflow customizing, only tasks that need to be done. Or not: let the heroes decide :)

There are the following user types:

- users: able to see all and create tasks (title and description)
- heroes: as users plus ability to modify all in tasks in their scopes
- admin: define scopes, used for tasks, as well as heroes allowed to work on each scope 

A task is made of:

- scope: one among the scopes defined by the admin
- title: mandatory, updatable, single line, 1 up to 255 chars
- description: optional, updatable, multi line, up to 1024 chars
- creator: mandatory, fixed at creation
- id: unique among all tasks, fixed at creation, made of the scope id, a dash and a unique number, 2 to 73 chars
- assignees: can be empty, updatable
- a task can be pending, in progress or done: default to pending
    - each state can be moved to any of the 2 others
    - a hero marking a task as in progress is automatically added as assignee

Multiple tasks with same title or description can be created.

A scope has:
- a name, 1 up to 255 chars, unique among all scopes, updatable, must be unique
- an id, unique among all scopes, by default deduced from the name, 1 to 36 chars, must be unique
 - can't be changed

User management should be linkable to a preexisting user directory.

# License

See [License](https://github.com/ManoManoTech/hexa-playground/LICENSE).

# Code of conduct

See [Code of conduct](https://github.com/ManoManoTech/hexa-playground/CODE_OF_CONDUCT.md).

# Contributing

See [Contributing](https://github.com/ManoManoTech/ALaMano/blob/master/CONTRIBUTING.md).