package at.priv.graf.zazentimer.di

import at.priv.graf.zazentimer.service.CoroutineDispatchers
import at.priv.graf.zazentimer.service.SystemClock
import at.priv.graf.zazentimer.service.ZazenClock
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class],
)
object TestDispatchersModule {
    @Provides
    @Singleton
    fun provideZazenClock(): ZazenClock = SystemClock()

    @Provides
    @Singleton
    fun provideCoroutineDispatchers(): CoroutineDispatchers {
        val testDispatcher = UnconfinedTestDispatcher()
        return CoroutineDispatchers(
            default = testDispatcher,
            io = testDispatcher,
            main = testDispatcher,
        )
    }
}
