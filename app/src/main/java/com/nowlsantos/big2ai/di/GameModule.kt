package com.nowlsantos.big2ai.di

import com.nowlsantos.big2ai.state.GameStateMachine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import com.nowlsantos.big2ai.engine.ExpertBig2AI

@Module
@InstallIn(SingletonComponent::class)
object GameModule {
    @Provides
    @Singleton
    @ApplicationScope // This will correctly refer to the ApplicationScope from its own file
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    // NEW: Provide the MutableMap<String, ExpertBig2AI> dependency
    // This provides an empty mutable map at injection time.
    // AI player instances would then typically be added to this map later,
    // e.g., during game setup in a ViewModel.
    @Provides
    @Singleton
    fun provideAiPlayersMap(): MutableMap<String, ExpertBig2AI> {
        return mutableMapOf()
    }

    @Provides
    @Singleton
    fun provideStateMachine(
        @ApplicationScope appScope: CoroutineScope,
        // NEW: Inject the provided aiPlayers map
        aiPlayers: MutableMap<String, ExpertBig2AI>
    ): GameStateMachine {
        // Pass the required aiPlayers map to the GameStateMachine constructor.
        // Other parameters (initialGameState, cardTracker, handEvaluator, opponentModel)
        // will use their default values as defined in the GameStateMachine constructor.
        return GameStateMachine(
            scope = appScope,
            aiPlayers = aiPlayers
        )
    }
}