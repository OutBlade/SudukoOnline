package de.sudokuonline.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.sudokuonline.app.data.model.Difficulty
import de.sudokuonline.app.data.model.GameMode
import de.sudokuonline.app.data.model.RoomStatus
import de.sudokuonline.app.ui.screens.*
import de.sudokuonline.app.viewmodel.AuthViewModel
import de.sudokuonline.app.viewmodel.GameViewModel
import de.sudokuonline.app.viewmodel.LobbyViewModel
import de.sudokuonline.app.viewmodel.TicTacToeViewModel
import de.sudokuonline.app.viewmodel.TicTacToeLobbyViewModel
import de.sudokuonline.app.viewmodel.LeaderboardViewModel
import de.sudokuonline.app.viewmodel.DameViewModel
import de.sudokuonline.app.viewmodel.DameLobbyViewModel
import de.sudokuonline.app.data.repository.SettingsRepository
import de.sudokuonline.app.data.repository.AchievementsRepository
import de.sudokuonline.app.data.repository.AchievementEvent
import de.sudokuonline.app.data.repository.CurrencyRepository
import de.sudokuonline.app.data.repository.CoinReason
import de.sudokuonline.app.data.repository.FriendsRepository
import de.sudokuonline.app.data.repository.Friend
import de.sudokuonline.app.data.repository.Challenge
import de.sudokuonline.app.data.repository.LootboxRepository
import de.sudokuonline.app.data.repository.ThemeRepository
import de.sudokuonline.app.data.model.OwnedLootbox
import de.sudokuonline.app.util.rememberSoundManager

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Home : Screen("home")
    object SinglePlayerGame : Screen("single_player_game/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "single_player_game/${difficulty.name}"
    }
    object ResumeGame : Screen("resume_game")
    object Lobby : Screen("lobby")
    object WaitingRoom : Screen("waiting_room/{roomId}") {
        fun createRoute(roomId: String) = "waiting_room/$roomId"
    }
    object MultiplayerGame : Screen("multiplayer_game/{roomId}") {
        fun createRoute(roomId: String) = "multiplayer_game/$roomId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")

    // TicTacToe screens
    object TicTacToeLobby : Screen("tictactoe_lobby")
    object TicTacToeWaitingRoom : Screen("tictactoe_waiting_room/{roomId}") {
        fun createRoute(roomId: String) = "tictactoe_waiting_room/$roomId"
    }
    object TicTacToeGame : Screen("tictactoe_game/{roomId}") {
        fun createRoute(roomId: String) = "tictactoe_game/$roomId"
    }
    object TicTacToeAI : Screen("tictactoe_ai")

    // Leaderboard
    object Leaderboard : Screen("leaderboard")

    // Muhle (Nine Men's Morris) screens
    object MuhleAI : Screen("muhle_ai")

    // Statistics screen
    object Statistics : Screen("statistics")

    // Daily Challenge screen
    object DailyChallenge : Screen("daily_challenge")

    // Tutorial screen
    object Tutorial : Screen("tutorial")

    // Puzzle Editor screen
    object PuzzleEditor : Screen("puzzle_editor")

    // Achievements screen
    object Achievements : Screen("achievements")
    
    // Theme Shop screen
    object ThemeShop : Screen("theme_shop")

    // Themes screen
    object Themes : Screen("themes")
    
    // Statistics Dashboard
    object StatisticsDashboard : Screen("statistics_dashboard")
    
    // Killer Sudoku
    object KillerSudoku : Screen("killer_sudoku/{difficulty}") {
        fun createRoute(difficulty: Difficulty) = "killer_sudoku/${difficulty.name}"
    }
    
    // League Leaderboard
    object LeagueLeaderboard : Screen("league_leaderboard")
    
    // Friends System
    object Friends : Screen("friends")
    object CreateChallenge : Screen("create_challenge/{friendId}/{friendName}") {
        fun createRoute(friendId: String, friendName: String) = "create_challenge/$friendId/$friendName"
    }
    object ChallengeDetails : Screen("challenge_details/{challengeId}")
    object MyChallenges : Screen("my_challenges")
    
    // Share App
    object ShareApp : Screen("share_app")

    // Online Games overview
    object OnlineGames : Screen("online_games")

    // Lootbox
    object Lootbox : Screen("lootbox")

    // Brainrot Wordle
    object BrainrotWordle : Screen("brainrot_wordle")

    // 2048
    object Game2048 : Screen("game_2048")

    // Math Trainer
    object MathTrainer : Screen("math_trainer")

    // Exam Simulator
    object ExamSimulator : Screen("exam_simulator")

    // LEN Trainer
    object LENQuiz : Screen("len_quiz")
    object LENTrainer : Screen("len_trainer")

    // HM2 Trainer
    object HM2Trainer : Screen("hm2_trainer")

    // Splash
    object Splash : Screen("splash")

    // Blackjack
    object Blackjack : Screen("blackjack")

    // Flip 7
    object Flip7 : Screen("flip_7")

    // Dame (Checkers) screens
    object DameAI : Screen("dame_ai")
    object DameLobby : Screen("dame_lobby")
    object DameWaitingRoom : Screen("dame_waiting_room/{roomId}") {
        fun createRoute(roomId: String) = "dame_waiting_room/$roomId"
    }
    object DameGame : Screen("dame_game/{roomId}") {
        fun createRoute(roomId: String) = "dame_game/$roomId"
    }
}

@Composable
fun SudokuNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel(),
    settingsRepository: SettingsRepository? = null
) {
    val authState by authViewModel.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    val dest = if (authState.isLoggedIn) Screen.Home.route else Screen.Auth.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Auth Screen
        composable(Screen.Auth.route) {
            AuthScreen(
                isLoading = authState.isLoading,
                error = authState.error,
                onSignIn = { email, password -> 
                    authViewModel.signIn(email, password) 
                },
                onRegister = { email, password, displayName ->
                    authViewModel.register(email, password, displayName)
                },
                onAnonymousSignIn = { 
                    authViewModel.signInAnonymously() 
                },
                onClearError = { authViewModel.clearError() }
            )
            
            // Navigate to home when logged in
            LaunchedEffect(authState.isLoggedIn) {
                if (authState.isLoggedIn) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            }
        }
        
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                playerName = authState.player?.displayName ?: "Spieler",
                onSinglePlayerClick = { difficulty ->
                    navController.navigate(Screen.SinglePlayerGame.createRoute(difficulty))
                },
                onOnlineClick = {
                    navController.navigate(Screen.OnlineGames.route)
                },
                onTicTacToeAIClick = {
                    navController.navigate(Screen.TicTacToeAI.route)
                },
                onMuhleAIClick = {
                    navController.navigate(Screen.MuhleAI.route)
                },
                onDameAIClick = {
                    navController.navigate(Screen.DameAI.route)
                },
                onLeaderboardClick = {
                    navController.navigate(Screen.Leaderboard.route)
                },
                onStatisticsClick = {
                    navController.navigate(Screen.Statistics.route)
                },
                onDailyChallengeClick = {
                    navController.navigate(Screen.DailyChallenge.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onContinueGameClick = {
                    navController.navigate(Screen.ResumeGame.route)
                },
                onTutorialClick = {
                    navController.navigate(Screen.Tutorial.route)
                },
                onThemeClick = {
                    navController.navigate(Screen.ThemeShop.route)
                },
                onAchievementsClick = {
                    navController.navigate(Screen.Achievements.route)
                },
                onStatisticsDashboardClick = {
                    navController.navigate(Screen.StatisticsDashboard.route)
                },
                onKillerSudokuClick = { difficulty ->
                    navController.navigate(Screen.KillerSudoku.createRoute(difficulty))
                },
                onLeagueLeaderboardClick = {
                    navController.navigate(Screen.LeagueLeaderboard.route)
                },
                onFriendsClick = {
                    navController.navigate(Screen.Friends.route)
                },
                onMyChallengesClick = {
                    navController.navigate(Screen.MyChallenges.route)
                },
                onShareAppClick = {
                    navController.navigate(Screen.ShareApp.route)
                },
                onLootboxClick = {
                    navController.navigate(Screen.Lootbox.route)
                },
                onBrainrotWordleClick = {
                    navController.navigate(Screen.BrainrotWordle.route)
                },
                on2048Click = {
                    navController.navigate(Screen.Game2048.route)
                },
                onMathTrainerClick = {
                    navController.navigate(Screen.MathTrainer.route)
                },
                onExamSimulatorClick = {
                    navController.navigate(Screen.ExamSimulator.route)
                },
                onLENQuizClick = {
                    navController.navigate(Screen.LENQuiz.route)
                },
                onLENTrainerClick = {
                    navController.navigate(Screen.LENTrainer.route)
                },
                onHM2TrainerClick = {
                    navController.navigate(Screen.HM2Trainer.route)
                },
                onBlackjackClick = {
                    navController.navigate(Screen.Blackjack.route)
                },
                onSevenClick = {
                    navController.navigate(Screen.Flip7.route)
                }
            )
        }

        // Resume Saved Game
        composable(Screen.ResumeGame.route) {
            val gameViewModel: GameViewModel = viewModel()
            val gameState by gameViewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            
            LaunchedEffect(Unit) {
                gameViewModel.initWithContext(context)
                if (!gameViewModel.resumeSavedGame()) {
                    // No saved game found, go back
                    navController.popBackStack()
                }
            }
            
            if (gameState.board.solution.flatten().any { it != 0 }) {
                GameScreen(
                    board = gameState.board,
                    selectedCell = gameState.selectedCell,
                    isNotesMode = gameState.isNotesMode,
                    errors = gameState.errors,
                    maxErrors = gameState.maxErrors,
                    hintsUsed = gameState.hintsUsed,
                    maxHints = gameState.maxHints,
                    elapsedSeconds = gameState.elapsedSeconds,
                    difficulty = gameState.difficulty,
                    gameMode = GameMode.SINGLE_PLAYER,
                    isPaused = gameState.isPaused,
                    isComplete = gameState.isComplete,
                    showGameOver = gameState.showGameOver,
                    isWinner = gameState.isWinner,
                    myProgress = 0,
                    opponentProgress = 0,
                    opponentName = "",
                    playerId = "",
                    onCellClick = { row, col -> gameViewModel.selectCell(row, col) },
                    onNumberClick = { number -> gameViewModel.enterNumber(number) },
                    onClearClick = { gameViewModel.clearCell() },
                    onNotesToggle = { gameViewModel.toggleNotesMode() },
                    onUndoClick = { gameViewModel.undo() },
                    onHintClick = { gameViewModel.useHint() },
                    onPauseClick = { gameViewModel.pauseAndSave() },
                    onBackClick = {
                        gameViewModel.resetGame()
                        navController.popBackStack()
                    },
                    onGameOverDismiss = {
                        gameViewModel.clearSavedGame()
                        gameViewModel.resetGame()
                        navController.popBackStack()
                    },
                    onPlayAgain = {
                        gameViewModel.clearSavedGame()
                        gameViewModel.startSinglePlayerGame(gameState.difficulty)
                    },
                    showAdForHintDialog = gameState.showAdForHintDialog,
                    bonusHints = gameState.bonusHints,
                    onAdRewardEarned = { gameViewModel.onAdRewardEarned() },
                    onDismissAdDialog = { gameViewModel.dismissAdDialog() }
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Single Player Game
        composable(
            route = Screen.SinglePlayerGame.route,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "MEDIUM"
            val difficulty = Difficulty.valueOf(difficultyName)
            
            val gameViewModel: GameViewModel = viewModel()
            val gameState by gameViewModel.state.collectAsState()
            val context = androidx.compose.ui.platform.LocalContext.current
            val soundManager = rememberSoundManager()
            val achievementsRepository = remember { AchievementsRepository.getInstance(context) }
            val currencyRepository = remember { CurrencyRepository.getInstance(context) }
            
            // Track previous errors for sound effect
            var previousErrors by remember { mutableIntStateOf(0) }
            // Track if coins were already awarded to prevent double-awarding
            var coinsAwarded by remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                gameViewModel.startSinglePlayerGame(difficulty)
                soundManager.playGameStart()
                achievementsRepository.checkAchievements(AchievementEvent.GamesPlayed)
            }
            
            // Play sound on error
            LaunchedEffect(gameState.errors) {
                if (gameState.errors > previousErrors) {
                    soundManager.playError()
                }
                previousErrors = gameState.errors
            }
            
            // Play sound on game completion and award coins
            LaunchedEffect(gameState.showGameOver, gameState.isComplete, gameState.isWinner) {
                if (gameState.showGameOver) {
                    if (gameState.isComplete && gameState.isWinner == true) {
                        soundManager.playWin()
                        achievementsRepository.checkAchievements(
                            AchievementEvent.GameWon(
                                gameType = "SUDOKU",
                                difficulty = gameState.difficulty.name,
                                timeSeconds = gameState.elapsedSeconds,
                                errors = gameState.errors,
                                hintsUsed = gameState.hintsUsed
                            )
                        )
                        // Award coins based on difficulty (only once)
                        if (!coinsAwarded) {
                            val coinReason = when (gameState.difficulty) {
                                Difficulty.EASY -> CoinReason.COMPLETE_EASY
                                Difficulty.MEDIUM -> CoinReason.COMPLETE_MEDIUM
                                Difficulty.HARD -> CoinReason.COMPLETE_HARD
                                Difficulty.EXPERT -> CoinReason.COMPLETE_EXPERT
                            }
                            currencyRepository.addCoins(coinReason.baseAmount, coinReason)
                            // Bonus for perfect game (no errors)
                            if (gameState.errors == 0) {
                                currencyRepository.addCoins(
                                    CoinReason.PERFECT_GAME.baseAmount, 
                                    CoinReason.PERFECT_GAME
                                )
                            }
                            coinsAwarded = true
                        }
                    } else if (gameState.isWinner == false) {
                        soundManager.playLose()
                    }
                }
            }
            
            GameScreen(
                board = gameState.board,
                selectedCell = gameState.selectedCell,
                isNotesMode = gameState.isNotesMode,
                errors = gameState.errors,
                maxErrors = gameState.maxErrors,
                hintsUsed = gameState.hintsUsed,
                maxHints = gameState.maxHints,
                elapsedSeconds = gameState.elapsedSeconds,
                difficulty = gameState.difficulty,
                gameMode = GameMode.SINGLE_PLAYER,
                isPaused = gameState.isPaused,
                isComplete = gameState.isComplete,
                showGameOver = gameState.showGameOver,
                isWinner = gameState.isWinner,
                myProgress = 0,
                opponentProgress = 0,
                opponentName = "",
                playerId = "",
                onCellClick = { row, col -> 
                    soundManager.playTap()
                    gameViewModel.selectCell(row, col) 
                },
                onNumberClick = { number -> 
                    soundManager.playPlace()
                    gameViewModel.enterNumber(number) 
                },
                onClearClick = { gameViewModel.clearCell() },
                onNotesToggle = { gameViewModel.toggleNotesMode() },
                onUndoClick = { 
                    soundManager.playUndo()
                    gameViewModel.undo() 
                },
                onHintClick = { 
                    soundManager.playHint()
                    gameViewModel.useHint() 
                },
                onPauseClick = { gameViewModel.togglePause() },
                onBackClick = {
                    gameViewModel.resetGame()
                    navController.popBackStack()
                },
                onGameOverDismiss = {
                    gameViewModel.resetGame()
                    navController.popBackStack()
                },
                onPlayAgain = {
                    soundManager.playGameStart()
                    gameViewModel.startSinglePlayerGame(difficulty)
                },
                onNavigateToTutorial = {
                    navController.navigate(Screen.Tutorial.route)
                },
                showAdForHintDialog = gameState.showAdForHintDialog,
                bonusHints = gameState.bonusHints,
                onAdRewardEarned = { gameViewModel.onAdRewardEarned() },
                onDismissAdDialog = { gameViewModel.dismissAdDialog() }
            )
        }

        // Lobby Screen
        composable(Screen.Lobby.route) {
            val lobbyViewModel: LobbyViewModel = viewModel()
            val lobbyState by lobbyViewModel.state.collectAsState()
            
            LaunchedEffect(Unit) {
                lobbyViewModel.loadAvailableRooms(lobbyState.selectedGameMode)
            }
            
            LobbyScreen(
                availableRooms = lobbyState.availableRooms,
                selectedGameMode = lobbyState.selectedGameMode,
                selectedDifficulty = lobbyState.selectedDifficulty,
                isSearching = lobbyState.isSearching,
                isLoading = lobbyState.isLoading,
                error = lobbyState.error,
                onGameModeChange = { lobbyViewModel.setGameMode(it) },
                onDifficultyChange = { lobbyViewModel.setDifficulty(it) },
                onCreateRoom = { isPrivate ->
                    val playerId = authState.user?.uid ?: return@LobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.createRoom(playerId, playerName, isPrivate)
                },
                onJoinRoom = { roomId ->
                    val playerId = authState.user?.uid ?: return@LobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.joinRoom(roomId, playerId, playerName)
                },
                onJoinByCode = { code ->
                    val playerId = authState.user?.uid ?: return@LobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.joinRoomByCode(code, playerId, playerName)
                },
                onStartMatchmaking = {
                    val playerId = authState.user?.uid ?: return@LobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.startMatchmaking(playerId, playerName)
                },
                onCancelMatchmaking = {
                    val playerId = authState.user?.uid ?: return@LobbyScreen
                    lobbyViewModel.cancelMatchmaking(playerId)
                },
                onBackClick = {
                    lobbyViewModel.resetState()
                    navController.popBackStack()
                },
                onClearError = { lobbyViewModel.clearError() }
            )
            
            // Navigate to waiting room when room is created or joined
            LaunchedEffect(lobbyState.createdRoom, lobbyState.joinedRoom) {
                val room = lobbyState.createdRoom ?: lobbyState.joinedRoom
                if (room != null) {
                    navController.navigate(Screen.WaitingRoom.createRoute(room.id))
                    lobbyViewModel.clearNavigation()
                }
            }
        }
        
        // Waiting Room Screen
        composable(
            route = Screen.WaitingRoom.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val playerId = authState.user?.uid ?: return@composable
            
            val gameViewModel: GameViewModel = viewModel()
            val gameState by gameViewModel.state.collectAsState()
            
            LaunchedEffect(roomId) {
                val playerName = authState.player?.displayName ?: "Spieler"
                // Only observe, player already joined via lobby
                gameViewModel.observeRoomOnly(roomId, playerId, playerName)
            }
            
            val room = gameState.room
            
            if (room != null) {
                // Navigate to game when room status changes to IN_PROGRESS
                LaunchedEffect(room.status) {
                    if (room.status == RoomStatus.IN_PROGRESS.name) {
                        navController.navigate(Screen.MultiplayerGame.createRoute(roomId)) {
                            popUpTo(Screen.WaitingRoom.route) { inclusive = true }
                        }
                    }
                }
                
                WaitingRoomScreen(
                    room = room,
                    currentPlayerId = playerId,
                    onStartGame = { gameViewModel.startGame() },
                    onLeaveRoom = {
                        gameViewModel.leaveRoom()
                        navController.popBackStack()
                    }
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        // Multiplayer Game Screen
        composable(
            route = Screen.MultiplayerGame.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val playerId = authState.user?.uid ?: return@composable
            
            val gameViewModel: GameViewModel = viewModel()
            val gameState by gameViewModel.state.collectAsState()
            
            LaunchedEffect(roomId) {
                val playerName = authState.player?.displayName ?: "Spieler"
                // Only observe, player already joined via lobby
                gameViewModel.observeRoomOnly(roomId, playerId, playerName)
            }
            
            val room = gameState.room
            val opponent = room?.players?.values?.firstOrNull { it.playerId != playerId }
            
            GameScreen(
                board = gameState.board,
                selectedCell = gameState.selectedCell,
                isNotesMode = gameState.isNotesMode,
                errors = gameState.errors,
                maxErrors = gameState.maxErrors,
                hintsUsed = gameState.hintsUsed,
                maxHints = gameState.maxHints,
                elapsedSeconds = gameState.elapsedSeconds,
                difficulty = gameState.difficulty,
                gameMode = gameState.gameMode,
                isPaused = gameState.isPaused,
                isComplete = gameState.isComplete,
                showGameOver = gameState.showGameOver,
                isWinner = gameState.isWinner,
                myProgress = room?.players?.get(playerId)?.progress ?: 0,
                opponentProgress = gameState.opponentProgress,
                opponentName = opponent?.displayName ?: "",
                playerId = playerId,
                onCellClick = { row, col -> gameViewModel.selectCell(row, col) },
                onNumberClick = { number -> gameViewModel.enterNumber(number) },
                onClearClick = { gameViewModel.clearCell() },
                onNotesToggle = { gameViewModel.toggleNotesMode() },
                onUndoClick = { gameViewModel.undo() },
                onHintClick = { gameViewModel.useHint() },
                onPauseClick = { gameViewModel.togglePause() },
                onBackClick = {
                    gameViewModel.leaveRoom()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onGameOverDismiss = {
                    gameViewModel.leaveRoom()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onPlayAgain = {
                    gameViewModel.leaveRoom()
                    navController.navigate(Screen.Lobby.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }
        
        // Profile Screen
        composable(Screen.Profile.route) {
            ProfileScreen(
                player = authState.player,
                onUpdateName = { name -> authViewModel.updateDisplayName(name) },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Settings Screen
        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onBackClick = { navController.popBackStack() }
            )
        }

        // TicTacToe Lobby Screen
        composable(Screen.TicTacToeLobby.route) {
            val lobbyViewModel: TicTacToeLobbyViewModel = viewModel()
            val lobbyState by lobbyViewModel.state.collectAsState()

            TicTacToeLobbyScreen(
                availableRooms = lobbyState.availableRooms,
                selectedGameMode = lobbyState.selectedGameMode,
                selectedBoardSize = lobbyState.selectedBoardSize,
                isLoading = lobbyState.isLoading,
                error = lobbyState.error,
                onGameModeChange = { lobbyViewModel.setGameMode(it) },
                onBoardSizeChange = { lobbyViewModel.setBoardSize(it) },
                onCreateRoom = { isPrivate ->
                    val playerId = authState.user?.uid ?: return@TicTacToeLobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.createRoom(playerId, playerName, isPrivate)
                },
                onJoinRoom = { roomId ->
                    val playerId = authState.user?.uid ?: return@TicTacToeLobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.joinRoom(roomId, playerId, playerName)
                },
                onJoinByCode = { code ->
                    val playerId = authState.user?.uid ?: return@TicTacToeLobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.joinRoomByCode(code, playerId, playerName)
                },
                onBackClick = {
                    lobbyViewModel.resetState()
                    navController.popBackStack()
                },
                onClearError = { lobbyViewModel.clearError() }
            )

            // Navigate to waiting room when room is created or joined
            LaunchedEffect(lobbyState.createdRoom, lobbyState.joinedRoom) {
                val room = lobbyState.createdRoom ?: lobbyState.joinedRoom
                if (room != null) {
                    navController.navigate(Screen.TicTacToeWaitingRoom.createRoute(room.id))
                    lobbyViewModel.clearNavigation()
                }
            }
        }

        // TicTacToe Waiting Room Screen
        composable(
            route = Screen.TicTacToeWaitingRoom.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val playerId = authState.user?.uid ?: return@composable

            val gameViewModel: TicTacToeViewModel = viewModel()
            val gameState by gameViewModel.state.collectAsState()

            LaunchedEffect(roomId) {
                val playerName = authState.player?.displayName ?: "Spieler"
                gameViewModel.observeRoomOnly(roomId, playerId, playerName)
            }

            val room = gameState.room

            if (room != null) {
                // Navigate to game when room status changes to IN_PROGRESS
                LaunchedEffect(room.status) {
                    if (room.status == RoomStatus.IN_PROGRESS.name) {
                        navController.navigate(Screen.TicTacToeGame.createRoute(roomId)) {
                            popUpTo(Screen.TicTacToeWaitingRoom.route) { inclusive = true }
                        }
                    }
                }

                TicTacToeWaitingRoomScreen(
                    room = room,
                    currentPlayerId = playerId,
                    onStartGame = { gameViewModel.startGame() },
                    onLeaveRoom = {
                        gameViewModel.leaveRoom()
                        navController.popBackStack()
                    }
                )
            } else {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // TicTacToe Game Screen
        composable(
            route = Screen.TicTacToeGame.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val playerId = authState.user?.uid ?: return@composable

            val gameViewModel: TicTacToeViewModel = viewModel()
            val gameState by gameViewModel.state.collectAsState()

            LaunchedEffect(roomId) {
                val playerName = authState.player?.displayName ?: "Spieler"
                gameViewModel.observeRoomOnly(roomId, playerId, playerName)
            }

            TicTacToeGameScreen(
                board = gameState.board,
                winningLine = gameState.winningLine,
                ultimateBoard = gameState.ultimateBoard,
                playableBoards = gameState.playableBoards,
                gameMode = gameState.gameMode,
                boardSize = gameState.boardSize,
                isMyTurn = gameState.isMyTurn,
                mySymbol = gameState.mySymbol,
                myName = gameState.playerName,
                opponentName = gameState.opponentName,
                bombsRemaining = gameState.bombsRemaining,
                opponentBombsRemaining = gameState.opponentBombsRemaining,
                actionMode = gameState.actionMode,
                elapsedSeconds = gameState.elapsedSeconds,
                isComplete = gameState.isComplete,
                isDraw = gameState.isDraw,
                winnerId = gameState.winnerId,
                playerId = playerId,
                showGameOver = gameState.showGameOver,
                // Rematch parameters
                rematchStatus = gameState.rematchStatus,
                rematchRequestedByMe = gameViewModel.didIRequestRematch(),
                mySeriesWins = gameState.mySeriesWins,
                opponentSeriesWins = gameState.opponentSeriesWins,
                roundNumber = gameState.roundNumber,
                onRequestRematch = { gameViewModel.requestRematch() },
                onAcceptRematch = { gameViewModel.acceptRematch() },
                onDeclineRematch = { gameViewModel.declineRematch() },
                // Other callbacks
                onCellClick = { row, col -> gameViewModel.onCellClick(row, col) },
                onUltimateCellClick = { boardRow, boardCol, cellRow, cellCol ->
                    gameViewModel.onUltimateCellClick(boardRow, boardCol, cellRow, cellCol)
                },
                onActionModeChange = { gameViewModel.setActionMode(it) },
                onBackClick = {
                    gameViewModel.leaveRoom()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onGameOverDismiss = {
                    gameViewModel.leaveRoom()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onPlayAgain = {
                    gameViewModel.leaveRoom()
                    navController.navigate(Screen.TicTacToeLobby.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        // Leaderboard Screen
        composable(Screen.Leaderboard.route) {
            val leaderboardViewModel: LeaderboardViewModel = viewModel()
            val leaderboardState by leaderboardViewModel.state.collectAsState()
            val playerId = authState.user?.uid ?: ""

            LaunchedEffect(Unit) {
                leaderboardViewModel.loadLeaderboard(playerId)
            }

            LeaderboardScreen(
                leaderboard = leaderboardState.leaderboard,
                currentPlayerId = playerId,
                isLoading = leaderboardState.isLoading,
                onBackClick = { navController.popBackStack() },
                onRefresh = { leaderboardViewModel.refresh(playerId) }
            )
        }

        // TicTacToe AI Screen
        composable(Screen.TicTacToeAI.route) {
            TicTacToeAIScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Muhle AI Screen
        composable(Screen.MuhleAI.route) {
            MuhleAIScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Statistics Screen
        composable(Screen.Statistics.route) {
            StatisticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Daily Challenge Screen
        composable(Screen.DailyChallenge.route) {
            DailyChallengeScreen(
                onBackClick = { navController.popBackStack() },
                onStartChallenge = { difficulty, board, solution ->
                    // Navigate to game with daily challenge data
                    navController.navigate(Screen.SinglePlayerGame.createRoute(difficulty))
                }
            )
        }
        
        // Tutorial Screen
        composable(Screen.Tutorial.route) {
            TutorialScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Theme Shop Screen
        composable(Screen.ThemeShop.route) {
            ThemeShopScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Achievements Screen
        composable(Screen.Achievements.route) {
            AchievementsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Statistics Dashboard Screen
        composable(Screen.StatisticsDashboard.route) {
            StatisticsDashboardScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Killer Sudoku Screen
        composable(
            route = Screen.KillerSudoku.route,
            arguments = listOf(
                navArgument("difficulty") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val difficultyName = backStackEntry.arguments?.getString("difficulty") ?: "MEDIUM"
            val difficulty = Difficulty.valueOf(difficultyName)
            
            KillerSudokuScreen(
                difficulty = difficulty,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // League Leaderboard Screen
        composable(Screen.LeagueLeaderboard.route) {
            LeagueLeaderboardScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        
        // Friends Screen
        composable(Screen.Friends.route) {
            FriendsScreen(
                onBackClick = { navController.popBackStack() },
                onChallengeClick = { friend ->
                    navController.navigate(
                        Screen.CreateChallenge.createRoute(friend.oderId, friend.displayName)
                    )
                },
                onActivityClick = { activity ->
                    activity.challengeId?.let { challengeId ->
                        navController.navigate("challenge_details/$challengeId")
                    }
                }
            )
        }
        
        // Create Challenge Screen
        composable(
            route = Screen.CreateChallenge.route,
            arguments = listOf(
                navArgument("friendId") { type = NavType.StringType },
                navArgument("friendName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val friendId = backStackEntry.arguments?.getString("friendId") ?: ""
            val friendName = backStackEntry.arguments?.getString("friendName") ?: ""
            
            CreateChallengeScreen(
                friend = Friend(oderId = friendId, displayName = friendName),
                onBackClick = { navController.popBackStack() },
                onChallengeSent = {
                    navController.popBackStack()
                    navController.popBackStack() // Go back to friends screen
                }
            )
        }
        
        // My Challenges Screen
        composable(Screen.MyChallenges.route) {
            MyChallengesScreen(
                onBackClick = { navController.popBackStack() },
                onChallengeClick = { challenge ->
                    // Navigate to challenge details or start game
                    navController.navigate("challenge_details/${challenge.id}")
                }
            )
        }
        
        // Challenge Details Screen
        composable(
            route = Screen.ChallengeDetails.route,
            arguments = listOf(navArgument("challengeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: return@composable
            val context = androidx.compose.ui.platform.LocalContext.current
            val friendsRepository = remember { FriendsRepository.getInstance(context) }
            val playerId = authState.user?.uid ?: return@composable

            var challenge by remember { mutableStateOf<Challenge?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(challengeId) {
                friendsRepository.getChallengeById(challengeId) { result ->
                    challenge = result
                    isLoading = false
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (challenge != null) {
                ChallengeDetailsScreen(
                    challenge = challenge!!,
                    onBackClick = { navController.popBackStack() },
                    onAccept = {
                        friendsRepository.acceptChallenge(challenge!!) {
                            // Navigate to game based on game type
                            when (challenge!!.gameType) {
                                "SUDOKU", "KILLER_SUDOKU" -> {
                                    val diff = try { Difficulty.valueOf(challenge!!.difficulty) } catch (e: Exception) { Difficulty.MEDIUM }
                                    navController.navigate(Screen.SinglePlayerGame.createRoute(diff)) {
                                        popUpTo(Screen.Home.route)
                                    }
                                }
                                else -> navController.popBackStack()
                            }
                        }
                    },
                    onDecline = {
                        friendsRepository.declineChallenge(challenge!!)
                        navController.popBackStack()
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Text("Herausforderung nicht gefunden")
                }
            }
        }

        // Share App Screen
        composable(Screen.ShareApp.route) {
            ShareAppScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Lootbox Screen
        composable(Screen.Lootbox.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val lootboxRepository = remember { LootboxRepository.getInstance(context) }
            val currencyRepository = remember { CurrencyRepository.getInstance(context) }
            val themeRepository = remember { ThemeRepository.getInstance(context) }

            var openingLootbox by remember { mutableStateOf<OwnedLootbox?>(null) }
            var openingRewards by remember { mutableStateOf<List<de.sudokuonline.app.data.model.LootboxReward>>(emptyList()) }

            val ownedBoxes by lootboxRepository.ownedLootboxes.collectAsState()

            if (openingLootbox != null) {
                LootboxOpeningScreen(
                    lootbox = openingLootbox!!,
                    rewards = openingRewards,
                    onDone = {
                        openingLootbox = null
                        openingRewards = emptyList()
                    },
                    onOpenAnother = if (ownedBoxes.isNotEmpty()) {
                        {
                            val next = ownedBoxes.first()
                            val rewards = lootboxRepository.openLootbox(next.id, currencyRepository, themeRepository)
                            openingLootbox = next
                            openingRewards = rewards
                        }
                    } else null
                )
            } else {
                LootboxScreen(
                    onBackClick = { navController.popBackStack() },
                    onOpenLootbox = { lootbox ->
                        val rewards = lootboxRepository.openLootbox(lootbox.id, currencyRepository, themeRepository)
                        openingLootbox = lootbox
                        openingRewards = rewards
                    }
                )
            }
        }

        // Online Games Screen
        composable(Screen.OnlineGames.route) {
            OnlineGamesScreen(
                onBackClick = { navController.popBackStack() },
                onSudokuMultiplayerClick = {
                    navController.navigate(Screen.Lobby.route)
                },
                onTicTacToeClick = {
                    navController.navigate(Screen.TicTacToeLobby.route)
                },
                onDameOnlineClick = {
                    navController.navigate(Screen.DameLobby.route)
                }
            )
        }

        // Challenge Details Screen
        composable(
            route = Screen.ChallengeDetails.route,
            arguments = listOf(
                navArgument("challengeId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val challengeId = backStackEntry.arguments?.getString("challengeId") ?: return@composable
            val context = androidx.compose.ui.platform.LocalContext.current
            val friendsRepository = remember { FriendsRepository.getInstance(context) }
            val challenges by friendsRepository.activeChallenges.collectAsState()
            val challenge = challenges.firstOrNull { it.id == challengeId }

            if (challenge != null) {
                ChallengeDetailsScreen(
                    challenge = challenge,
                    onBackClick = { navController.popBackStack() },
                    onAccept = { navController.popBackStack() },
                    onDecline = { navController.popBackStack() }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        // Brainrot Wordle Screen
        composable(Screen.BrainrotWordle.route) {
            BrainrotWordleScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // 2048 Screen
        composable(Screen.Game2048.route) {
            Game2048Screen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Blackjack Screen
        composable(Screen.Blackjack.route) {
            BlackjackScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Flip 7 Screen
        composable(Screen.Flip7.route) {
            Flip7Screen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Math Trainer Screen
        composable(Screen.MathTrainer.route) {
            MathTrainerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Exam Simulator Screen
        composable(Screen.ExamSimulator.route) {
            ExamSimulatorScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // LEN Quiz Screen
        composable(Screen.LENQuiz.route) {
            LENQuizScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // LEN MC Trainer Screen
        composable(Screen.LENTrainer.route) {
            LENTrainerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // HM2 Trainer Screen
        composable(Screen.HM2Trainer.route) {
            Hm2TrainerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Dame AI Screen
        composable(Screen.DameAI.route) {
            DameAIScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // Dame Lobby Screen
        composable(Screen.DameLobby.route) {
            val lobbyViewModel: DameLobbyViewModel = viewModel()
            val lobbyState by lobbyViewModel.state.collectAsState()

            LaunchedEffect(Unit) {
                lobbyViewModel.loadAvailableRooms()
            }

            DameLobbyScreen(
                availableRooms = lobbyState.availableRooms,
                isLoading = lobbyState.isLoading,
                error = lobbyState.error,
                onCreateRoom = { isPrivate ->
                    val playerId = authState.user?.uid ?: return@DameLobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.createRoom(playerId, playerName, isPrivate)
                },
                onJoinRoom = { roomId ->
                    val playerId = authState.user?.uid ?: return@DameLobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.joinRoom(roomId, playerId, playerName)
                },
                onJoinByCode = { code ->
                    val playerId = authState.user?.uid ?: return@DameLobbyScreen
                    val playerName = authState.player?.displayName ?: "Spieler"
                    lobbyViewModel.joinRoomByCode(code, playerId, playerName)
                },
                onBackClick = {
                    lobbyViewModel.resetState()
                    navController.popBackStack()
                },
                onClearError = { lobbyViewModel.clearError() }
            )

            LaunchedEffect(lobbyState.createdRoom, lobbyState.joinedRoom) {
                val room = lobbyState.createdRoom ?: lobbyState.joinedRoom
                if (room != null) {
                    navController.navigate(Screen.DameWaitingRoom.createRoute(room.id))
                    lobbyViewModel.clearNavigation()
                }
            }
        }

        // Dame Waiting Room Screen
        composable(
            route = Screen.DameWaitingRoom.route,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val playerId = authState.user?.uid ?: return@composable

            val dameViewModel: DameViewModel = viewModel()
            val dameState by dameViewModel.state.collectAsState()

            LaunchedEffect(roomId) {
                val playerName = authState.player?.displayName ?: "Spieler"
                dameViewModel.observeRoom(roomId, playerId, playerName)
            }

            val room = dameState.room

            if (room != null) {
                LaunchedEffect(room.status) {
                    if (room.status == de.sudokuonline.app.data.model.RoomStatus.IN_PROGRESS.name) {
                        navController.navigate(Screen.DameGame.createRoute(roomId)) {
                            popUpTo(Screen.DameWaitingRoom.route) { inclusive = true }
                        }
                    }
                }

                DameWaitingRoomScreen(
                    room = room,
                    currentPlayerId = playerId,
                    onStartGame = { dameViewModel.startGame() },
                    onLeaveRoom = {
                        dameViewModel.leaveRoom()
                        navController.popBackStack()
                    }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Dame Online Game Screen
        composable(
            route = Screen.DameGame.route,
            arguments = listOf(navArgument("roomId") { type = NavType.StringType })
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: return@composable
            val playerId = authState.user?.uid ?: return@composable

            val dameViewModel: DameViewModel = viewModel()
            val dameState by dameViewModel.state.collectAsState()

            LaunchedEffect(roomId) {
                val playerName = authState.player?.displayName ?: "Spieler"
                dameViewModel.observeRoom(roomId, playerId, playerName)
            }

            DameGameScreen(
                room = dameState.room,
                playerId = playerId,
                selectedPiece = dameState.selectedPiece,
                validMoves = dameState.validMoves,
                isMyTurn = dameState.isMyTurn,
                myColor = dameState.myColor,
                elapsedSeconds = dameState.elapsedSeconds,
                isComplete = dameState.isComplete,
                winnerId = dameState.winnerId,
                isDraw = dameState.isDraw,
                showGameOver = dameState.showGameOver,
                rematchStatus = dameState.rematchStatus,
                rematchRequestedByMe = dameViewModel.didIRequestRematch(),
                onCellClick = { row, col -> dameViewModel.onCellClick(row, col) },
                onRequestRematch = { dameViewModel.requestRematch() },
                onAcceptRematch = { dameViewModel.acceptRematch() },
                onDeclineRematch = { dameViewModel.declineRematch() },
                onBackClick = {
                    dameViewModel.leaveRoom()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onGameOverDismiss = { dameViewModel.dismissGameOver() },
                onPlayAgain = {
                    dameViewModel.requestRematch()
                }
            )
        }
    }
}
