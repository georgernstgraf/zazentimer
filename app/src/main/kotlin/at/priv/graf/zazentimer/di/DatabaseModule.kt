package at.priv.graf.zazentimer.di

import at.priv.graf.zazentimer.service.CoroutineDispatchers
import at.priv.graf.zazentimer.service.DbMeditationRepository
import at.priv.graf.zazentimer.service.MeditationRepository
import at.priv.graf.zazentimer.service.SystemClock
import at.priv.graf.zazentimer.service.ZazenClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun provideZazenClock(): ZazenClock = SystemClock()

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers = CoroutineDispatchers()
}

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindMeditationRepository(impl: DbMeditationRepository): MeditationRepository
}
