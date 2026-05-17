package dev.trinitychurch.lyrics.app.di

import dev.trinitychurch.lyrics.domain.PresentationStateStore
import org.koin.dsl.module

val AppModule = module {
    single { PresentationStateStore() }
}
