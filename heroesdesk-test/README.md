# Heroes Desk domain - Kotlin Arrow implementation Hexagonal testing

As described in the [heroesdesk module README.MD](../heroesdesk/README.md), this module provides reusable tests cases for the domain and its adapters.

These tests are in the 'org.hexastacks.heroesdesk.kotlin.test.AbstractHeroesDeskTest' class. 

To be able to quickly iterate on the model and have an easy to manipulate reference implementation, the TaskRepository and UserRepository ports are implemented in memory:
```
org.hexastacks.heroesdesk.kotlin.adapters.inmemory.TaskRepositoryInMemory
org.hexastacks.heroesdesk.kotlin.adapters.inmemory.UserRepositoryInMemory
```

Correspondingly, the default HeroesDesk test class is `org.hexastacks.heroesdesk.kotlin.HeroesDeskImplTest` and uses the above in memory adapters.