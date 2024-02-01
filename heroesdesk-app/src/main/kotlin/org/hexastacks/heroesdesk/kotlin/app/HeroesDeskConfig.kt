package org.hexastacks.heroesdesk.kotlin.app

import org.hexastacks.heroesdesk.kotlin.HeroesDesk
import org.hexastacks.heroesdesk.kotlin.misc.HeroesDeskImpl
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InMemoryMissionRepository
import org.hexastacks.heroesdesk.kotlin.ports.inmemory.InstrumentedInMemoryUserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class HeroesDeskConfig {

    @Bean
    open fun instrumentedInMemoryUserRepository(): InstrumentedInMemoryUserRepository =
        InstrumentedInMemoryUserRepository()

    @Bean
    open fun heroesDesk(instrumentedInMemoryUserRepository: InstrumentedInMemoryUserRepository): HeroesDesk {
        return HeroesDeskImpl(instrumentedInMemoryUserRepository, InMemoryMissionRepository())
    }
}