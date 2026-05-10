package at.priv.graf.zazentimer.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

import at.priv.graf.zazentimer.service.SystemClock
import at.priv.graf.zazentimer.service.ZazenClock
import dagger.Provides
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideZazenClock(): ZazenClock = SystemClock()
}
