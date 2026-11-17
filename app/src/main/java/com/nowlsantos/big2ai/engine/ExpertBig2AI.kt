package com.nowlsantos.big2ai.engine

import com.nowlsantos.big2ai.model.enums.ComboType
import com.nowlsantos.big2ai.model.states.GameState
import com.nowlsantos.big2ai.model.data.Move
import com.nowlsantos.big2ai.model.data.PassMove
import com.nowlsantos.big2ai.model.data.PlayMove
import com.nowlsantos.big2ai.model.data.Player
import com.nowlsantos.big2ai.model.data.PlayerHandCombination
import com.nowlsantos.big2ai.model.enums.PlayerPosition
import com.nowlsantos.big2ai.model.data.PlayingCard
import com.nowlsantos.big2ai.model.enums.Rank
import com.nowlsantos.big2ai.model.enums.Suit
import com.nowlsantos.big2ai.util.AppLogger
import kotlin.math.max


class ExpertBig2AI(
    private val playerId: String,
    private val cardTracker: CardTracker,
    private val opponentModel: OpponentModel,
    private val handEvaluator: BigTwoHandEvaluator
) {
    private val threeDiamonds = PlayingCard(Rank.THREE, Suit.DIAMONDS)
    private val tag = "ExpertBig2AI"
    private val MAX_STRENGTH = 15 * 10 + 3 // Maximum card strength (2 of Spades)

    // Professional-level strategy parameters
    private val highCardPenalties = mapOf(
        Rank.TWO to 3000.0,  // Extremely high penalty for 2s
        Rank.ACE to 1000.0,   // High penalty for Aces
        Rank.KING to 400.0,
        Rank.QUEEN to 200.0
    )

    private val ENDGAME_AGGRESSION_FACTOR = 3.0

    // Game state tracking
    private var lastKnownHand: List<PlayingCard> = emptyList()
    private var cachedCombinations: List<PlayerHandCombination> = emptyList()
    private var position: PlayerPosition? = null
    private val humanPassCounts = mutableMapOf<String, Int>()
    private var aggressionLevel = 65 // Default: moderately aggressive
    private var riskTolerance = 55   // Default: moderate risk tolerance
    private var gamePhase = GamePhase.MID_GAME
    private var consecutivePasses = 0
    private var trickHistory = mutableListOf<List<PlayingCard>>()

    private enum class GamePhase { EARLY_GAME, MID_GAME, END_GAME }

    fun makeMove(gameState: GameState): Move {
        val currentPlayer = gameState.players.first { it.id == playerId }
        updateGameState(gameState, currentPlayer)

        val allValidMoves = generateValidMoves(gameState, currentPlayer)
        val validPlayMoves = allValidMoves.filterIsInstance<PlayMove>()
        val canPass = allValidMoves.any { it is PassMove }

        // Track trick history for pattern recognition
        if (gameState.lastPlayedHand.isNotEmpty()) {
            trickHistory.add(gameState.lastPlayedHand)
            if (trickHistory.size > 5) trickHistory.removeAt(0)
        }

        // 1. Instant win detection
        validPlayMoves.firstOrNull { it.cards.size == currentPlayer.hand.size }?.let {
            AppLogger.d(tag, "Winning move: ${cardsToString(it.cards)}")
            return sortFiveCardMove(it)
        }

        // 2. First move handling
        if (gameState.isFirstMoveOfGame) {
            return handleFirstMove(validPlayMoves, currentPlayer)
        }

        // 3. Strategic pass evaluation
        val shouldPassResult = shouldPass(gameState, validPlayMoves, currentPlayer)
        if (canPass && shouldPassResult.first) {
            consecutivePasses++
            AppLogger.d(tag, "Strategic pass: ${shouldPassResult.second}")
            return PassMove(playerId)
        } else {
            consecutivePasses = 0
        }

        // 4. Endgame strategy
        if (isEndgame(gameState)) {
            return handleEndgameStrategy(gameState, validPlayMoves, currentPlayer)
        }

        // 5. Bomb strategy
        handleBombStrategy(gameState, validPlayMoves)?.let {
            AppLogger.d(tag, "Bomb play: ${cardsToString(it.cards)}")
            return sortFiveCardMove(it)
        }

        // 6. Positional strategy
        applyPositionalStrategy(gameState, validPlayMoves, currentPlayer)?.let {
            AppLogger.d(tag, "Positional play: ${cardsToString(it.cards)}")
            return sortFiveCardMove(it)
        }

        // 7. Optimal professional move selection
        return selectProfessionalMove(gameState, validPlayMoves, currentPlayer)
    }

    private fun updateGameState(gameState: GameState, player: Player) {
        updateGamePhase(gameState)
        updateHumanPassTracking(gameState)
        position = position ?: player.position
        updateStrategyParameters(gameState)
        cardTracker.update(gameState, playerId)
        opponentModel.update(gameState, playerId)
    }

    private fun updateGamePhase(gameState: GameState) {
        val totalCards = gameState.players.sumOf { it.hand.size }
        gamePhase = when {
            totalCards <= 8 -> GamePhase.END_GAME
            totalCards <= 20 -> GamePhase.MID_GAME
            else -> GamePhase.EARLY_GAME
        }
        AppLogger.d(tag, "Game phase: $gamePhase")
    }

    private fun updateHumanPassTracking(gameState: GameState) {
        if (gameState.lastPlayedHand.isEmpty()) {
            humanPassCounts.clear()
            consecutivePasses = 0
        }

        gameState.players.filter { it.isHuman }.forEach { human ->
            if (gameState.playersWhoPassedInCurrentTrick.contains(human.id)) {
                humanPassCounts[human.id] = humanPassCounts.getOrDefault(human.id, 1) + 1
            } else {
                humanPassCounts.remove(human.id)
            }
        }
    }

    private fun updateStrategyParameters(gameState: GameState) {
        val currentPlayer = gameState.players.first { it.id == playerId }
        val myCards = currentPlayer.hand.size
        val minOpponentCards = gameState.players
            .filter { it.id != playerId }
            .minOfOrNull { it.hand.size } ?: 13

        // Professional aggression model
        aggressionLevel = when {
            myCards <= 2 -> 95
            gameState.players.any { it.id != playerId && it.hand.size <= 2 } -> 90
            myCards <= minOpponentCards - 3 -> 85
            myCards <= 5 -> 80
            else -> 65
        }

        // Professional risk assessment
        riskTolerance = when {
            gameState.players.count { it.hand.size <= 3 } >= 2 -> 30
            myCards >= 12 -> 70
            position == PlayerPosition.SOUTH -> 60
            else -> 50
        }
        AppLogger.d(tag, "Strategy: Aggression=$aggressionLevel, Risk=$riskTolerance")
    }

    private fun generateValidMoves(
        gameState: GameState,
        player: Player
    ): List<Move> {
        if (player.hand != lastKnownHand) {
            lastKnownHand = player.hand.sortedWith(
                compareBy<PlayingCard> { it.rank.value }.thenBy { it.suit.ordinal }
            )
            cachedCombinations = handEvaluator.findCombinations(lastKnownHand)
        }

        val moves = mutableListOf<Move>()
        val currentTrickCombo = gameState.lastPlayedHand.takeIf { it.isNotEmpty() }?.let {
            handEvaluator.evaluateHand(it)
        }

        for (combo in cachedCombinations) {
            val playMove = PlayMove(combo.cards, combo.type, player.id)
            val evaluatedMoveCombo = handEvaluator.evaluateHand(playMove.cards) ?: continue

            if (gameState.isFirstMoveOfGame) {
                if (evaluatedMoveCombo.cards.contains(threeDiamonds)) moves.add(playMove)
            } else {
                when {
                    currentTrickCombo == null -> moves.add(playMove)
                    handEvaluator.beats(evaluatedMoveCombo, currentTrickCombo) -> moves.add(playMove)
                }
            }
        }

        if (!gameState.isFirstMoveOfGame && gameState.lastPlayedHand.isNotEmpty()) {
            moves.add(PassMove(player.id))
        }

        return moves.filter { move ->
            if (move is PlayMove) {
                when {
                    move.type.isBomb() && !isValidBomb(move) -> false
                    move.type == ComboType.FULL_HOUSE && move.cards.any { card ->
                        (card.rank == Rank.TWO || card.rank == Rank.ACE) &&
                                move.cards.count { it.rank == card.rank } == 2
                    } && gamePhase != GamePhase.END_GAME -> false
                    else -> true
                }
            } else true
        }
    }

    private fun handleFirstMove(moves: List<PlayMove>, player: Player): Move {
        moves.filter { it.cards.contains(threeDiamonds) }.minByOrNull {
            professionalMoveScore(it, player)
        }?.let {
            return sortFiveCardMove(it)
        }

        AppLogger.e(tag, "No 3♦ move! Hand: ${cardsToString(player.hand)}")
        return moves.filter { it.cards.contains(threeDiamonds) }
            .minByOrNull { moveScore(it) } ?: PassMove(playerId)
    }

    private fun professionalMoveScore(move: PlayMove, player: Player): Int {
        // Professional first move: Prefer moves that preserve flexibility
        val remaining = player.hand - move.cards
        return when (move.type) {
            ComboType.TRIPLE -> 0
            ComboType.PAIR -> 10
            ComboType.SINGLE -> 20
            else -> 50
        } + remaining.count { it.rank.value > 10 } * 5
    }

    private fun handleEndgameStrategy(
        gameState: GameState,
        moves: List<PlayMove>,
        player: Player
    ): Move {
        // Professional endgame: Aggressive when ahead, conservative when behind
        val playerPosition = gameState.players.sortedBy { it.hand.size }
        val myPosition = playerPosition.indexOfFirst { it.id == playerId }
        val isAhead = myPosition < playerPosition.size / 2

        return when {
            gameState.lastPlayedHand.isEmpty() -> {
                // Leading in endgame
                if (isAhead) {
                    moves.minByOrNull { expertHeuristicScore(it, gameState) }?.let {
                        AppLogger.d(tag, "Endgame lead (ahead): ${cardsToString(it.cards)}")
                        sortFiveCardMove(it)
                    } ?: PassMove(playerId)
                } else {
                    moves.maxByOrNull { expertHeuristicScore(it, gameState) }?.let {
                        AppLogger.d(tag, "Endgame lead (behind): ${cardsToString(it.cards)}")
                        sortFiveCardMove(it)
                    } ?: PassMove(playerId)
                }
            }
            else -> {
                // Following in endgame
                moves.filter { canBeatCurrentTrick(it, gameState) }.minByOrNull {
                    if (isAhead) expertHeuristicScore(it, gameState)
                    else -expertHeuristicScore(it, gameState)
                }?.let {
                    AppLogger.d(tag, "Endgame counter: ${cardsToString(it.cards)}")
                    sortFiveCardMove(it)
                } ?: PassMove(playerId)
            }
        }
    }

    private fun isEndgame(state: GameState): Boolean {
        val player = state.players.first { it.id == playerId }
        return player.hand.size <= 4 || state.players.any { it.hand.size <= 2 }
    }

    private fun selectProfessionalMove(
        gameState: GameState,
        moves: List<PlayMove>,
        player: Player
    ): Move {
        if (moves.isEmpty()) {
            return if (gameState.lastPlayedHand.isNotEmpty())
                PassMove(playerId)
            else
                moves.minByOrNull { moveScore(it) } ?: PassMove(playerId)
        }

        val currentTrickCombo = gameState.lastPlayedHand.takeIf { it.isNotEmpty() }?.let {
            handEvaluator.evaluateHand(it)
        }

        // Professional leading strategy
        if (currentTrickCombo == null) {
            return when (gamePhase) {
                GamePhase.EARLY_GAME -> leadEarlyGame(moves, player, gameState)
                GamePhase.MID_GAME -> leadMidGame(moves, player, gameState)
                GamePhase.END_GAME -> leadEndGame(moves, player, gameState)
            }
        }

        // Professional following strategy
        return followTrick(moves, gameState, player)
    }

    private fun leadEarlyGame(moves: List<PlayMove>, player: Player, gameState: GameState): Move {
        // Professional early game: Shed low cards, preserve combinations
        val lowMoves = moves.filter {
            it.cards.all { card -> card.rank.value <= Rank.EIGHT.value } &&
                    !breaksValuableCombo(it, player)
        }

        return lowMoves.minByOrNull { expertHeuristicScore(it, gameState) }
            ?: moves.minByOrNull { expertHeuristicScore(it, gameState) }
            ?: moves.minByOrNull { moveScore(it) } ?: PassMove(playerId)
    }

    private fun leadMidGame(moves: List<PlayMove>, player: Player, gameState: GameState): Move {
        // Professional mid-game: Balance card reduction and combo preservation
        val safeMoves = moves.filter { !breaksValuableCombo(it, player) }
        return safeMoves.minByOrNull { expertHeuristicScore(it, gameState) }
            ?: moves.minByOrNull { expertHeuristicScore(it, gameState) }
            ?: moves.minByOrNull { moveScore(it) } ?: PassMove(playerId)
    }

    private fun leadEndGame(moves: List<PlayMove>, player: Player, gameState: GameState): Move {
        // Professional endgame: Maximize winning potential
        return moves.minByOrNull {
            // Ensure consistent return type (Double) for the comparison
            if (it.cards.size == player.hand.size) Double.NEGATIVE_INFINITY
            else expertHeuristicScore(it, gameState)
        } ?: moves.minByOrNull { calculateMoveValue(it.cards) }
        ?: PassMove(playerId)
    }

    private fun followTrick(
        moves: List<PlayMove>,
        gameState: GameState,
        player: Player
    ): Move {
        val beatableMoves = moves.filter { canBeatCurrentTrick(it, gameState) }
        if (beatableMoves.isEmpty()) return PassMove(playerId)

        // Get next player ID for opponent modeling
        val nextPlayerId = getNextPlayerId(gameState, player.id)
        // Ensure opponentModel.estimateHandStrength is called with a non-null ID
        val nextPlayerStrength = nextPlayerId?.let { opponentModel.estimateHandStrength(it) } ?: 0.0

        return beatableMoves.minByOrNull { move ->
            // Prioritize playing the lowest possible beating move
            // This encourages shedding low cards or small combinations
            val cardValueSum = move.cards.sumOf { it.strength } // Sum of individual card strengths
            val comboTypePenalty = when (move.type) {
                ComboType.SINGLE -> 0
                ComboType.PAIR -> 10
                ComboType.TRIPLE -> 20
                ComboType.STRAIGHT, ComboType.FLUSH, ComboType.FULL_HOUSE -> 50
                ComboType.FOUR_OF_A_KIND, ComboType.STRAIGHT_FLUSH -> 100 // Penalize using bombs unnecessarily
                else -> 0
            }

            // Higher penalty for playing high cards if not strictly necessary
            val highCardExcessPenalty = move.cards.filter { it.rank.value >= Rank.QUEEN.value }.size * 20

            // Favor moves that are just enough to beat, minimizing overkill
            val currentTrickStrength = gameState.lastPlayedHand.sumOf { it.strength }
            val overkillFactor = if (cardValueSum > currentTrickStrength) (cardValueSum - currentTrickStrength) * 0.5 else 0.0

            cardValueSum + comboTypePenalty + highCardExcessPenalty + overkillFactor
        } ?: beatableMoves.first()
    }

    private fun breaksValuableCombo(move: PlayMove, player: Player): Boolean {
        val remaining = player.hand - move.cards
        val remainingCombos = handEvaluator.findCombinations(remaining)
        // Check if we're breaking potential straights/flushes
        return cachedCombinations.any { combo -> (combo.type == ComboType.STRAIGHT || combo.type == ComboType.FLUSH) && !remainingCombos.any { it.cards.containsAll(combo.cards) } }
    }

    private fun expertHeuristicScore(
        move: PlayMove,
        state: GameState
    ): Double {
        var score = 0.0
        val player = state.players.first { it.id == playerId }
        val isMyEndgame = isEndgame(state) // Use isMyEndgame for clarity
        val isHumanPlayerEndgame = state.players.any { it.isHuman && it.hand.size <= 3 } // Check if any human is in endgame

        // Evaluate the last played hand if it exists to understand the current trick
        val currentTrickCombo = state.lastPlayedHand.takeIf { it.isNotEmpty() }?.let { handEvaluator.evaluateHand(it) }

        // 1. Base Score for Combo Type (incentivize playing certain types to shed)
        score += when (move.type) {
            ComboType.SINGLE -> 15.0
            ComboType.PAIR -> 50.0
            ComboType.TRIPLE -> 100.0
            ComboType.STRAIGHT -> 200.0
            ComboType.FLUSH -> 300.0
            ComboType.FULL_HOUSE -> 400.0
            ComboType.FOUR_OF_A_KIND -> 500.0
            ComboType.STRAIGHT_FLUSH -> 1000.0
            else -> 0.0
        }

        // 2. Bonus for reducing card count (always good to shed cards)
        score -= move.cards.size * 10.0 // Stronger incentive to shed more cards

        // 3. Dynamic High-Card Conservation / Aggression
        val highCardCost = move.cards.sumOf { card ->
            highCardPenalties[card.rank]?.let { basePenalty ->
                when {
                    // In endgame (AI's or human's), significantly reduce cost for high cards to encourage playing them aggressively.
                    isMyEndgame || isHumanPlayerEndgame -> basePenalty * 0.05

                    // If leading with a 2 or Ace, apply heavy cost (to discourage)
                    currentTrickCombo == null && (card.rank == Rank.TWO || card.rank == Rank.ACE) ->
                        basePenalty * 2.0

                    // When beating an existing trick with a 2 or Ace (strategic conservation/aggression)
                    currentTrickCombo != null && (card.rank == Rank.TWO || card.rank == Rank.ACE) -> {
                        val opponentLeadRank = currentTrickCombo.leadCard?.rank?.value ?: 0
                        if (opponentLeadRank >= Rank.ACE.value) { // Beating an Ace or Two with another high card (necessary high-stakes counter)
                            basePenalty * 0.1 // Very low cost, as it's a high-stakes counter
                        } else {
                            basePenalty * 0.5 // Moderate cost for using on lower high cards (e.g. King, Queen)
                        }
                    }
                    // Default cost for high cards in non-endgame, non-specific scenarios
                    else -> basePenalty * 1.0
                }
            } ?: 0.0
        }
        score += highCardCost

        // 4. Remaining Hand Quality (Junk Card Penalty)
        val remainingHand = player.hand.minus(move.cards.toSet())
        val remainingSingles = handEvaluator.findCombinations(remainingHand).filter { it.type == ComboType.SINGLE }
        val junkCardPenalty = remainingSingles.count { it.leadCard.rank.value <= Rank.SIX.value } * 20.0 // Penalize leaving low singles
        score += junkCardPenalty

        // 5. Opponent Threat Awareness
        val threateningOpponents = state.players.filter { it.id != playerId && opponentModel.isPlayerCloseToWinning(it.id) }
        if (threateningOpponents.isNotEmpty()) {
            // If playing this move allows an opponent to win or significantly advance, penalize
            val wouldGiveLeadToThreat = currentTrickCombo != null && handEvaluator.beats(handEvaluator.evaluateHand(move.cards)!!, currentTrickCombo) &&
                    threateningOpponents.any { it.id == getNextPlayerId(state, playerId) } // Corrected: use internal helper
            if (wouldGiveLeadToThreat) {
                score += 500.0 // Heavy penalty
            }
            // Bonus for playing strong cards to stop threatening opponents
            if (move.cards.any { it.rank == Rank.TWO || it.rank == Rank.ACE } && currentTrickCombo != null && canBeatCurrentTrick(move, state)) {
                score -= 100.0 // Incentive to use high cards to defend
            }
        }

        // 6. Bomb Overplay Penalty
        if (move.type.isBomb() && currentTrickCombo != null && !currentTrickCombo.type.isBomb()) {
            val cheaperBeatingMovesExist = state.players.first { it.id == playerId }.hand.minus(move.cards.toSet())
                .let { remainingHand ->
                    handEvaluator.findCombinations(remainingHand).any { combo ->
                        combo.type != ComboType.FOUR_OF_A_KIND && combo.type != ComboType.STRAIGHT_FLUSH &&
                                handEvaluator.beats(combo, currentTrickCombo)
                    }
                }
            if (cheaperBeatingMovesExist) {
                score += 700.0 // Heavy penalty for using bomb unnecessarily
            }
        }

        // 7. Combo Preservation Penalty (more refined)
        if (!isMyEndgame) {
            val originalFiveCardCombos = cachedCombinations.count { it.type.isFiveCardCombo() }
            val remainingFiveCardCombos = handEvaluator.findCombinations(remainingHand).count { it.type.isFiveCardCombo() }
            if (originalFiveCardCombos > remainingFiveCardCombos) {
                score += (originalFiveCardCombos - remainingFiveCardCombos) * 250.0 // Heavy penalty for breaking potential big combos
            }
        }

        // 8. Positional Advantage (from previous version, kept)
        when (position) {
            PlayerPosition.SOUTH -> score -= 20.0
            PlayerPosition.NORTH -> score += 10.0
            else -> {}
        }

        // 9. Trick pattern recognition (from previous version, kept)
        if (trickHistory.size >= 3) {
            val lastTrickType = handEvaluator.evaluateHand(trickHistory.last())?.type
            if (lastTrickType != null && lastTrickType == move.type) {
                score -= 30.0
            }
        }
        return score
    }

    private fun calculateMoveStrength(cards: List<PlayingCard>): Double {
        return cards.sumOf { calculateCardStrength(it) }
    }

    private fun calculateCardStrength(card: PlayingCard): Double {
        return when (card.rank) {
            Rank.TWO -> 15.0
            Rank.ACE -> 14.0
            Rank.KING -> 13.0
            Rank.QUEEN -> 12.0
            Rank.JACK -> 11.0
            else -> card.rank.value.toDouble()
        } * (if (card.suit == Suit.SPADES) 1.2 else 1.0)
    }

    private fun shouldPass(
        state: GameState,
        moves: List<PlayMove>,
        player: Player
    ): Pair<Boolean, String> {
        if (state.lastPlayedHand.isEmpty()) return false to "Cannot pass when leading"
        if (moves.isEmpty()) return true to "No valid moves"

        val currentTrickCombo = state.lastPlayedHand.takeIf { true }?.let { handEvaluator.evaluateHand(it) } ?: return false to "No current trick"

        val isMyEndgame = isEndgame(state)
        val isHumanPlayerEndgame = state.players.any { it.isHuman && it.hand.size <= 3 }

        // 1. Never pass in endgame if there's *any* valid move, unless it's a bomb that can't be beaten
        if (isMyEndgame || isHumanPlayerEndgame) {
            if ((currentTrickCombo.type.isBomb()) &&
                moves.none { (it.type.isBomb()) && canBeatCurrentTrick(it, state) }) {
                return true to "Endgame: Cannot beat opponent's bomb."
            }
            if (moves.isNotEmpty()) {
                return false to "Endgame: Must play to win or pressure."
            }
        }

        // 2. Cannot beat opponent's bomb (if currentTrick is a bomb)
        if ((currentTrickCombo.type.isBomb()) &&
            moves.none { (it.type.isBomb()) && canBeatCurrentTrick(it, state) }) {
            return true to "Cannot beat opponent's bomb."
        }

        // 3. CRITICAL: Don't pass on low-value tricks if a non-high-card beating move exists.
        // This directly addresses the "passing on low-ranking cards" issue.
        val isLowRankedTrick = currentTrickCombo.leadCard.rank.value <= Rank.TEN.value
        val isLowValueComboType = when (currentTrickCombo.type) {
            ComboType.SINGLE, ComboType.PAIR, ComboType.TRIPLE -> true
            else -> false
        }
        if (isLowRankedTrick && isLowValueComboType) {
            val hasCheapBeatingMove = moves.any { move ->
                val usesHighCard = move.cards.any { card -> card.rank == Rank.TWO || card.rank == Rank.ACE || card.rank == Rank.KING || card.rank == Rank.QUEEN }
                !usesHighCard && canBeatCurrentTrick(move, state)
            }
            if (hasCheapBeatingMove) {
                return false to "Cannot pass on a low-ranked trick if a cheap beating move exists."
            }
        }

        // 4. Strategic conservation: Pass to save high cards (2s, Aces) or bombs if the current trick is not threatening
        // and a cheaper alternative is not available.
        val hasHighCardsToSave = player.hand.any { it.rank == Rank.TWO || it.rank == Rank.ACE }
        val hasBombToSave = cachedCombinations.any { it.type.isBomb() }
        val currentTrickIsWeak = currentTrickCombo.leadCard.rank.value <= Rank.QUEEN.value && !currentTrickCombo.type.isBomb()

        if ((hasHighCardsToSave || hasBombToSave) && currentTrickIsWeak) {
            val allBeatingMovesAreExpensive = moves.all { move ->
                val usesHighCard = move.cards.any { card -> card.rank == Rank.TWO || card.rank == Rank.ACE }
                val isBombMove = move.type.isBomb()
                usesHighCard || isBombMove || breaksValuableCombo(move, player)
            }
            if (allBeatingMovesAreExpensive) {
                return true to "Conserving high cards/bombs on a weak trick."
            }
        }

        // 5. Strategic observation pass (less common, only if AI has strong hand and not pressured)
        if (consecutivePasses < 2 &&
            state.playersWhoPassedInCurrentTrick.size < 2 &&
            player.hand.size > 7 &&
            !isMyEndgame &&
            state.players.none { (it.id != playerId) && (it.hand.size <= 3) } &&
            riskTolerance > 50) {
            val bestPlayScore = moves.minByOrNull { expertHeuristicScore(it, state) }?.let { expertHeuristicScore(it, state) } ?: Double.MIN_VALUE
            if (bestPlayScore < 100.0) { // If best play is not very strong
                return true to "Strategic observation pass (low pressure)."
            }
        }

        // 6. Avoid trapping self: If passing leaves an unplayable hand (e.g., only high cards or broken combos)
        val hypotheticalRemainingHand = player.hand.minus(moves.flatMap { it.cards }.toSet())
        val hypotheticalRemainingCombos = handEvaluator.findCombinations(hypotheticalRemainingHand)
        val wouldBeTrapped = hypotheticalRemainingHand.isNotEmpty() &&
                hypotheticalRemainingCombos.all { it.type == ComboType.SINGLE && it.leadCard.rank.value > Rank.QUEEN.value } // Only high singles left
        if (wouldBeTrapped) {
            return false to "Avoid trapping self by passing."
        }

        return false to "Default: No strong reason to pass"
    }

    private fun handleBombStrategy(gameState: GameState, moves: List<PlayMove>): PlayMove? {
        val currentTrickCombo = gameState.lastPlayedHand.takeIf { it.isNotEmpty() }?.let {
            handEvaluator.evaluateHand(it)
        }

        // Find all bomb moves available
        val bombMoves = moves.filter { it.type.isBomb() }

        if (bombMoves.isEmpty()) return null

        // If leading, prefer to save bombs unless it's a winning move
        if (currentTrickCombo == null) {
            return bombMoves.firstOrNull { it.cards.size == gameState.players.first { p -> p.id == playerId }.hand.size }
        }

        // If current trick is a bomb, play a higher bomb
        if (currentTrickCombo.type.isBomb()) {
            return bombMoves.filter { handEvaluator.beats(handEvaluator.evaluateHand(it.cards)!!, currentTrickCombo) }
                .maxByOrNull { it.type.rankOrder * 100 + (it.leadCard()?.strength ?: 0) } // Prioritize stronger bombs using rankOrder
        }

        // If current trick is not a bomb but strong, consider using a bomb
        val highestNonBombPlay = currentTrickCombo.leadCard.strength

        val potentialBombPlays = bombMoves.filter {
            // Only play bomb if we can't beat it with a normal combination
            !moves.any { m -> !m.type.isBomb() && handEvaluator.beats(handEvaluator.evaluateHand(m.cards)!!, currentTrickCombo) }
        }

        return potentialBombPlays.minByOrNull {
            // Prefer to use weaker bombs first, or bombs that will clear the table
            val estimatedNextPlayerStrength = opponentModel.estimateHandStrength(
                getNextPlayerId(gameState, playerId) ?: ""
            )
            val bombStrength = handEvaluator.evaluateHand(it.cards)!!.strength
            val remainingCards = gameState.players.first { p -> p.id == playerId }.hand.size - it.cards.size

            when {
                remainingCards == 0 -> -10000.0 // Winning move
                bombStrength > estimatedNextPlayerStrength && remainingCards <= 2 -> -5000.0 // Clear for win
                else -> bombStrength.toDouble() // Default: just strength
            }
        }
    }

    private fun applyPositionalStrategy(gameState: GameState, moves: List<PlayMove>, player: Player): PlayMove? {
        val currentTrickCombo = gameState.lastPlayedHand.takeIf { it.isNotEmpty() }?.let {
            handEvaluator.evaluateHand(it)
        }

        // If leading, no positional strategy specific to beating
        if (currentTrickCombo == null) return null

        val playersInOrder = gameState.playersInPlayOrder(gameState.lastPlayerIdWhoPlayed ?: gameState.currentPlayerId)
        val myIndex = playersInOrder.indexOfFirst { it.id == playerId }
        val lastPlayerIndex = playersInOrder.indexOfFirst { it.id == gameState.lastPlayerIdWhoPlayed }
        val playersAfterMe = playersInOrder.subList(myIndex + 1, playersInOrder.size)

        // If I am the last player to act, and can win, do it
        if (playersAfterMe.all { it.hasPassed || it.hand.isEmpty() } && player.hand.size == moves.firstOrNull { canBeatCurrentTrick(it, gameState) }?.cards?.size) {
            return moves.firstOrNull { canBeatCurrentTrick(it, gameState) && it.cards.size == player.hand.size }
        }

        // If I am next to last and the last player has many cards, try to play a mid-range card
        if (playersAfterMe.size == 1 && playersAfterMe.first().hand.size > 5) {
            return moves.filter { canBeatCurrentTrick(it, gameState) }
                .maxByOrNull { it.type.rankOrder * 100 + (it.leadCard()?.strength ?: 0) }
        }

        return null
    }

    private fun getNextPlayerId(gameState: GameState, currentId: String): String? {
        val players = gameState.players
        val currentIndex = players.indexOfFirst { it.id == currentId }
        if (currentIndex == -1) return null

        var nextIndex = (currentIndex + 1) % players.size
        var count = 0
        while (count < players.size) {
            val nextPlayer = players[nextIndex]
            // Only consider players who are still in the game (haven't passed for the current trick and still have cards)
            if (!nextPlayer.hasPassed && nextPlayer.hand.isNotEmpty()) {
                return nextPlayer.id
            }
            nextIndex = (nextIndex + 1) % players.size
            count++
        }
        return null // Should not happen in a valid game state where there's always a next player or trick ends
    }

    private fun canBeatCurrentTrick(move: PlayMove, gameState: GameState): Boolean {
        val currentTrickCombo = gameState.lastPlayedHand.takeIf { it.isNotEmpty() }?.let {
            handEvaluator.evaluateHand(it)
        } ?: return true // If no current trick, any move is valid

        val playedCombo = handEvaluator.evaluateHand(move.cards) ?: return false
        return handEvaluator.beats(playedCombo, currentTrickCombo)
    }

    private fun moveScore(move: PlayMove): Double {
        // Basic heuristic: prioritize playing smaller combos and lower cards
        return when (move.type) {
            ComboType.SINGLE -> move.cards.first().strength.toDouble()
            ComboType.PAIR -> move.cards.maxOf { it.strength }.toDouble() * 10
            ComboType.TRIPLE -> move.cards.maxOf { it.strength }.toDouble() * 100
            ComboType.STRAIGHT, ComboType.FLUSH, ComboType.FULL_HOUSE,
            ComboType.FOUR_OF_A_KIND, ComboType.STRAIGHT_FLUSH ->
                move.cards.sumOf { it.strength }.toDouble() + move.type.rankOrder * 1000.0
            else -> Double.MAX_VALUE // Should not happen for valid moves
        }
    }

    private fun isValidBomb(move: PlayMove): Boolean {
        // Implement Big Two specific bomb rules (e.g., Four of a Kind must be played on something, etc.)
        // This is a placeholder and should be integrated with actual game rules
        return true
    }

    // Helper function to convert list of cards to a string for logging
    private fun cardsToString(cards: List<PlayingCard>): String {
        return cards.joinToString(", ") { "${it.rank} of ${it.suit}" }
    }

    /**
     * Calculates a "move value" for the AI's potential play, used in heuristics.
     */
    private fun calculateMoveValue(cards: List<PlayingCard>): Double {
        if (cards.isEmpty()) return 0.0
        val combo = handEvaluator.evaluateHand(cards) ?: return 0.0

        var value = cards.sumOf { card ->
            when (card.rank) {
                Rank.TWO -> 25.0
                Rank.ACE -> 20.0
                Rank.KING -> 18.0
                Rank.QUEEN -> 16.0
                Rank.JACK -> 14.0
                else -> card.rank.value.toDouble()
            }
        }
        value += when(combo.type) {
            ComboType.SINGLE -> 0.0
            ComboType.PAIR -> 15.0
            ComboType.TRIPLE -> 30.0
            ComboType.STRAIGHT -> 50.0
            ComboType.FLUSH -> 60.0
            ComboType.FULL_HOUSE -> 70.0
            ComboType.FOUR_OF_A_KIND -> 90.0
            ComboType.STRAIGHT_FLUSH -> 120.0
            else -> 0.0
        }
        return value
    }

    // Helper to sort 5-card moves for consistent output/display
    private fun sortFiveCardMove(move: PlayMove): PlayMove {
        return if (move.cards.size == 5) {
            move.copy(cards = move.cards.sortedWith(
                compareBy<PlayingCard> { it.rank.value }.thenBy { it.suit.ordinal }
            ).toMutableList()) // Ensure it's a mutable list for PlayMove constructor if needed
        } else {
            move
        }
    }
}

// Extension function for GameState to provide players in play order
fun GameState.playersInPlayOrder(startingPlayerId: String): List<Player> {
    val startIndex = players.indexOfFirst { it.id == startingPlayerId }
    if (startIndex == -1) return players // Fallback if starting player not found

    return (players.subList(startIndex, players.size) + players.subList(0, startIndex))
        .filter { !it.hasPassed } // Filter out players who have passed for the current trick
}