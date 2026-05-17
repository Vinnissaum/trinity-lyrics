package dev.trinitylabs.lyrics.app.di

import dev.trinitylabs.lyrics.domain.PresentationStateStore
import org.koin.dsl.module

val AppModule = module {
    single { PresentationStateStore() }
}
