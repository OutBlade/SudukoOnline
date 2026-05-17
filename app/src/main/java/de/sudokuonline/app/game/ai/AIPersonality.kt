package de.sudokuonline.app.game.ai

/**
 * AI Personality System - Different playstyles for more interesting gameplay
 *
 * Each personality affects:
 * - Move selection preferences
 * - Risk tolerance
 * - Aggression level
 * - Strategic priorities
 */
enum class AIPersonality(
    val displayName: String,
    val description: String,
    val aggressionFactor: Float,      // 0.0 = very defensive, 1.0 = very aggressive
    val riskTolerance: Float,         // 0.0 = safe plays, 1.0 = risky plays
    val trickinessFactor: Float,      // 0.0 = straightforward, 1.0 = tricky/deceptive
    val centerPreference: Float,      // 0.0 = no preference, 1.0 = strongly prefer center
    val cornerPreference: Float,      // 0.0 = no preference, 1.0 = strongly prefer corners
    val millPriority: Float,          // For Mühle: 0.0 = low, 1.0 = aggressive mill hunting
    val blockingPriority: Float,      // 0.0 = ignore threats, 1.0 = always block
    val bombAggressiveness: Float     // For Bomb TicTacToe: when to use bombs
) {
    // Standard balanced AI
    BALANCED(
        displayName = "Ausgeglichen",
        description = "Spielt solide und macht keine offensichtlichen Fehler",
        aggressionFactor = 0.5f,
        riskTolerance = 0.5f,
        trickinessFactor = 0.3f,
        centerPreference = 0.6f,
        cornerPreference = 0.5f,
        millPriority = 0.6f,
        blockingPriority = 0.8f,
        bombAggressiveness = 0.5f
    ),

    // Aggressive attacker
    AGGRESSIVE(
        displayName = "Aggressiv",
        description = "Greift ständig an und versucht schnell zu gewinnen",
        aggressionFactor = 0.9f,
        riskTolerance = 0.7f,
        trickinessFactor = 0.4f,
        centerPreference = 0.8f,
        cornerPreference = 0.3f,
        millPriority = 0.9f,
        blockingPriority = 0.5f,  // Less focused on blocking
        bombAggressiveness = 0.8f
    ),

    // Defensive player
    DEFENSIVE(
        displayName = "Defensiv",
        description = "Konzentriert sich auf Verteidigung und wartet auf Fehler",
        aggressionFactor = 0.2f,
        riskTolerance = 0.2f,
        trickinessFactor = 0.2f,
        centerPreference = 0.7f,
        cornerPreference = 0.7f,
        millPriority = 0.4f,
        blockingPriority = 1.0f,  // Always blocks
        bombAggressiveness = 0.2f
    ),

    // Tricky/deceptive player
    TRICKY(
        displayName = "Trickreich",
        description = "Setzt Fallen und spielt unvorhersehbar",
        aggressionFactor = 0.6f,
        riskTolerance = 0.8f,
        trickinessFactor = 1.0f,
        centerPreference = 0.4f,
        cornerPreference = 0.6f,
        millPriority = 0.7f,
        blockingPriority = 0.6f,
        bombAggressiveness = 0.9f  // Loves using bombs unexpectedly
    ),

    // Positional player (controls the board)
    POSITIONAL(
        displayName = "Positionell",
        description = "Kontrolliert wichtige Felder und baut langsam Vorteile auf",
        aggressionFactor = 0.4f,
        riskTolerance = 0.3f,
        trickinessFactor = 0.2f,
        centerPreference = 1.0f,  // Strongly prefers center
        cornerPreference = 0.8f,
        millPriority = 0.5f,
        blockingPriority = 0.7f,
        bombAggressiveness = 0.4f
    ),

    // Chaotic/unpredictable
    CHAOTIC(
        displayName = "Chaotisch",
        description = "Völlig unberechenbar - manchmal genial, manchmal verrückt",
        aggressionFactor = 0.5f,
        riskTolerance = 1.0f,
        trickinessFactor = 0.9f,
        centerPreference = 0.3f,
        cornerPreference = 0.3f,
        millPriority = 0.5f,
        blockingPriority = 0.4f,
        bombAggressiveness = 1.0f
    ),

    // Grandmaster-like perfect play
    GRANDMASTER(
        displayName = "Großmeister",
        description = "Spielt nahezu perfekt - extrem schwer zu schlagen",
        aggressionFactor = 0.6f,
        riskTolerance = 0.4f,
        trickinessFactor = 0.5f,
        centerPreference = 0.7f,
        cornerPreference = 0.6f,
        millPriority = 0.8f,
        blockingPriority = 0.95f,
        bombAggressiveness = 0.6f
    );

    companion object {
        /**
         * Get a random personality for variety
         */
        fun random(): AIPersonality = entries.random()

        /**
         * Get personality by difficulty level
         */
        fun forDifficulty(strength: Int): AIPersonality {
            return when {
                strength <= 20 -> CHAOTIC
                strength <= 40 -> DEFENSIVE
                strength <= 60 -> BALANCED
                strength <= 80 -> AGGRESSIVE
                else -> GRANDMASTER
            }
        }
    }
}

/**
 * Modifies evaluation scores based on personality
 */
object PersonalityModifier {

    /**
     * Modify a move's score based on personality preferences
     */
    fun modifyScore(
        baseScore: Int,
        personality: AIPersonality,
        moveType: MoveType,
        gamePhase: GamePhase = GamePhase.MIDDLE
    ): Int {
        var score = baseScore.toFloat()

        when (moveType) {
            MoveType.WINNING -> {
                // Everyone likes winning
                score *= 1.0f + personality.aggressionFactor * 0.2f
            }
            MoveType.BLOCKING -> {
                score *= 0.5f + personality.blockingPriority * 0.8f
            }
            MoveType.CREATING_THREAT -> {
                score *= 0.6f + personality.aggressionFactor * 0.6f
            }
            MoveType.FORK -> {
                score *= 0.7f + personality.trickinessFactor * 0.5f + personality.aggressionFactor * 0.3f
            }
            MoveType.CENTER -> {
                score *= 0.5f + personality.centerPreference * 0.8f
            }
            MoveType.CORNER -> {
                score *= 0.5f + personality.cornerPreference * 0.7f
            }
            MoveType.MILL_CREATION -> {
                score *= 0.5f + personality.millPriority * 0.8f
            }
            MoveType.DOUBLE_MILL -> {
                score *= 0.6f + personality.millPriority * 0.6f + personality.trickinessFactor * 0.4f
            }
            MoveType.BOMB_ATTACK -> {
                score *= 0.3f + personality.bombAggressiveness * 1.0f
            }
            MoveType.RISKY -> {
                score *= 0.2f + personality.riskTolerance * 1.0f
            }
            MoveType.SAFE -> {
                score *= 0.5f + (1.0f - personality.riskTolerance) * 0.7f
            }
            MoveType.POSITIONAL -> {
                val positionalBonus = (personality.centerPreference + personality.cornerPreference) / 2f
                score *= 0.5f + positionalBonus * 0.7f
            }
        }

        // Phase adjustments
        when (gamePhase) {
            GamePhase.OPENING -> {
                // Aggressive personalities push harder in opening
                if (personality.aggressionFactor > 0.6f) {
                    score *= 1.1f
                }
            }
            GamePhase.MIDDLE -> {
                // Tricky personalities shine in middle game
                if (personality.trickinessFactor > 0.6f && moveType == MoveType.FORK) {
                    score *= 1.2f
                }
            }
            GamePhase.ENDGAME -> {
                // Everyone plays more carefully in endgame
                if (moveType == MoveType.BLOCKING || moveType == MoveType.WINNING) {
                    score *= 1.15f
                }
            }
        }

        return score.toInt()
    }

    /**
     * Add randomness based on personality (for less predictable play)
     */
    fun addPersonalityNoise(score: Int, personality: AIPersonality): Int {
        val noiseRange = (50 * personality.trickinessFactor + 20 * personality.riskTolerance).toInt()
        if (noiseRange <= 0) return score

        val noise = (-noiseRange..noiseRange).random()
        return score + noise
    }

    /**
     * Decide whether to make an unexpected move
     */
    fun shouldMakeUnexpectedMove(personality: AIPersonality): Boolean {
        val chance = personality.trickinessFactor * 0.15f + personality.riskTolerance * 0.05f
        return Math.random() < chance
    }
}

enum class MoveType {
    WINNING,           // Wins the game
    BLOCKING,          // Blocks opponent's win
    CREATING_THREAT,   // Creates a winning threat
    FORK,              // Creates multiple threats
    CENTER,            // Controls center
    CORNER,            // Controls corner
    MILL_CREATION,     // Creates a mill (Mühle)
    DOUBLE_MILL,       // Creates Zwickmühle
    BOMB_ATTACK,       // Uses bomb offensively
    RISKY,             // High risk, high reward move
    SAFE,              // Conservative, safe move
    POSITIONAL         // Improves board position
}

enum class GamePhase {
    OPENING,   // First few moves
    MIDDLE,    // Main game
    ENDGAME    // Few pieces left / close to end
}
