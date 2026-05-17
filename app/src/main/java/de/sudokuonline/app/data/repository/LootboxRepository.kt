package de.sudokuonline.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import de.sudokuonline.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

class LootboxRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lootbox_prefs", Context.MODE_PRIVATE)

    private val _ownedLootboxes = MutableStateFlow(loadLootboxes())
    val ownedLootboxes: StateFlow<List<OwnedLootbox>> = _ownedLootboxes.asStateFlow()

    private val _ownedEmojis = MutableStateFlow(loadOwnedEmojis())
    val ownedEmojis: StateFlow<Set<String>> = _ownedEmojis.asStateFlow()

    private val _selectedEmoji = MutableStateFlow(loadSelectedEmoji())
    val selectedEmoji: StateFlow<String?> = _selectedEmoji.asStateFlow()

    // Event for notifying UI about new lootbox
    private val _newLootboxEvent = MutableStateFlow<OwnedLootbox?>(null)
    val newLootboxEvent: StateFlow<OwnedLootbox?> = _newLootboxEvent.asStateFlow()

    fun clearNewLootboxEvent() {
        _newLootboxEvent.value = null
    }

    fun purchaseLootbox(rarity: LootboxRarity, currencyRepository: CurrencyRepository): Boolean {
        if (!currencyRepository.spendCoins(rarity.price)) return false
        addLootbox(rarity, "shop")
        return true
    }

    fun addLootbox(rarity: LootboxRarity, source: String) {
        val box = OwnedLootbox(
            id = UUID.randomUUID().toString(),
            rarity = rarity,
            source = source
        )
        val updated = _ownedLootboxes.value + box
        _ownedLootboxes.value = updated
        saveLootboxes(updated)
        _newLootboxEvent.value = box
    }

    fun openLootbox(
        lootboxId: String,
        currencyRepository: CurrencyRepository,
        themeRepository: ThemeRepository
    ): List<LootboxReward> {
        val box = _ownedLootboxes.value.find { it.id == lootboxId } ?: return emptyList()
        val ownedThemeIds = themeRepository.ownedThemes.value
        val rewards = generateRewards(box.rarity, ownedThemeIds)

        // Apply rewards
        for (reward in rewards) {
            when (reward.type) {
                LootboxRewardType.COINS -> {
                    currencyRepository.addCoins(reward.amount, CoinReason.LOOTBOX)
                }
                LootboxRewardType.THEME -> {
                    // Unlock theme via ThemeRepository internal method
                    unlockTheme(reward.id, themeRepository)
                }
                LootboxRewardType.EMOJI -> {
                    if (reward.id in _ownedEmojis.value) {
                        // Duplicate emoji → bonus coins
                        val bonusCoins = when (reward.rarity) {
                            RewardRarity.COMMON -> 25
                            RewardRarity.RARE -> 50
                            RewardRarity.EPIC -> 100
                            RewardRarity.LEGENDARY -> 250
                        }
                        currencyRepository.addCoins(bonusCoins, CoinReason.LOOTBOX)
                    } else {
                        val emojis = _ownedEmojis.value + reward.id
                        _ownedEmojis.value = emojis
                        saveOwnedEmojis(emojis)
                    }
                }
            }
        }

        // Remove opened box
        val updated = _ownedLootboxes.value.filter { it.id != lootboxId }
        _ownedLootboxes.value = updated
        saveLootboxes(updated)

        return rewards
    }

    private fun unlockTheme(themeId: String, themeRepository: ThemeRepository) {
        // Add to owned themes set in ThemeRepository prefs
        val owned = themeRepository.ownedThemes.value.toMutableSet()
        owned.add(themeId)
        themeRepository.forceSetOwnedThemes(owned)
    }

    fun setSelectedEmoji(emojiId: String?) {
        _selectedEmoji.value = emojiId
        prefs.edit().putString("selected_emoji", emojiId).apply()
    }

    fun getLootboxCount(): Int = _ownedLootboxes.value.size

    private fun generateRewards(rarity: LootboxRarity, ownedThemeIds: Set<String>): List<LootboxReward> {
        val rewards = mutableListOf<LootboxReward>()
        val count = rarity.rewardCount
        val grantedThemeIds = mutableSetOf<String>()

        // For legendary: guarantee at least 1 epic+ item
        var guaranteedRareAdded = false

        for (i in 0 until count) {
            val roll = Random.nextFloat()
            val reward = when {
                // Legendary box: guarantee epic+ emoji on first slot
                rarity == LootboxRarity.LEGENDARY && !guaranteedRareAdded -> {
                    guaranteedRareAdded = true
                    generateEmojiReward(minRarity = RewardRarity.EPIC)
                }
                // Theme chance
                roll < getThemeChance(rarity) -> generateThemeReward(rarity, ownedThemeIds + grantedThemeIds)
                // Emoji chance
                roll < getThemeChance(rarity) + getEmojiChance(rarity) -> generateEmojiReward(rarity = rarity)
                // Coins (default)
                else -> generateCoinReward(rarity)
            } ?: generateCoinReward(rarity) // fallback to coins if no theme/emoji available

            if (reward.type == LootboxRewardType.THEME) {
                grantedThemeIds.add(reward.id)
            }
            rewards.add(reward)
        }

        // Ensure at least 1 coin reward
        if (rewards.none { it.type == LootboxRewardType.COINS }) {
            rewards[rewards.lastIndex] = generateCoinReward(rarity)
        }

        return rewards
    }

    private fun getThemeChance(rarity: LootboxRarity): Float = when (rarity) {
        LootboxRarity.BRONZE -> 0.05f
        LootboxRarity.SILVER -> 0.15f
        LootboxRarity.GOLD -> 0.30f
        LootboxRarity.LEGENDARY -> 0.50f
    }

    private fun getEmojiChance(rarity: LootboxRarity): Float = when (rarity) {
        LootboxRarity.BRONZE -> 0.20f
        LootboxRarity.SILVER -> 0.30f
        LootboxRarity.GOLD -> 0.40f
        LootboxRarity.LEGENDARY -> 0.30f
    }

    private fun generateCoinReward(rarity: LootboxRarity): LootboxReward {
        val (min, max) = when (rarity) {
            LootboxRarity.BRONZE -> 15 to 50
            LootboxRarity.SILVER -> 50 to 150
            LootboxRarity.GOLD -> 150 to 400
            LootboxRarity.LEGENDARY -> 400 to 800
        }
        val amount = Random.nextInt(min, max + 1)
        val rewardRarity = when {
            amount > max * 0.8 -> RewardRarity.RARE
            amount > max * 0.5 -> RewardRarity.COMMON
            else -> RewardRarity.COMMON
        }
        return LootboxReward(
            type = LootboxRewardType.COINS,
            id = "coins",
            displayName = "$amount Coins",
            amount = amount,
            rarity = rewardRarity,
            icon = "\uD83D\uDCB0"
        )
    }

    private fun generateThemeReward(rarity: LootboxRarity, ownedThemeIds: Set<String> = emptySet()): LootboxReward? {
        val allPremiumThemes = listOf("ocean", "forest", "sunset", "lavender", "midnight", "cherry", "neon", "gold")
        val unowned = allPremiumThemes.filter { it !in ownedThemeIds }
        if (unowned.isEmpty()) return null

        val themeId = unowned[Random.nextInt(unowned.size)]
        val themeName = when (themeId) {
            "ocean" -> "Ozean"
            "forest" -> "Wald"
            "sunset" -> "Sonnenuntergang"
            "lavender" -> "Lavendel"
            "midnight" -> "Mitternacht"
            "cherry" -> "Kirschblüte"
            "neon" -> "Neon"
            "gold" -> "Gold"
            else -> themeId
        }
        val themeRarity = when (themeId) {
            "ocean", "forest", "lavender" -> RewardRarity.RARE
            "sunset", "cherry", "midnight" -> RewardRarity.EPIC
            "neon", "gold" -> RewardRarity.LEGENDARY
            else -> RewardRarity.COMMON
        }

        // Only allow legendary themes from gold+ boxes
        if (themeRarity == RewardRarity.LEGENDARY && rarity.ordinal < LootboxRarity.GOLD.ordinal) {
            return null
        }

        return LootboxReward(
            type = LootboxRewardType.THEME,
            id = themeId,
            displayName = "Theme: $themeName",
            rarity = themeRarity,
            icon = "\uD83C\uDFA8"
        )
    }

    private fun generateEmojiReward(
        rarity: LootboxRarity? = null,
        minRarity: RewardRarity = RewardRarity.COMMON
    ): LootboxReward? {
        val maxRarity = when (rarity) {
            LootboxRarity.BRONZE -> RewardRarity.RARE
            LootboxRarity.SILVER -> RewardRarity.EPIC
            LootboxRarity.GOLD -> RewardRarity.LEGENDARY
            LootboxRarity.LEGENDARY -> RewardRarity.LEGENDARY
            null -> RewardRarity.LEGENDARY
        }

        val available = ProfileEmoji.entries.filter {
            it.rarity.ordinal >= minRarity.ordinal && it.rarity.ordinal <= maxRarity.ordinal
        }
        if (available.isEmpty()) return null

        // Weighted random: common more likely
        val weights = available.map { emoji ->
            when (emoji.rarity) {
                RewardRarity.COMMON -> 40f
                RewardRarity.RARE -> 25f
                RewardRarity.EPIC -> 10f
                RewardRarity.LEGENDARY -> 3f
            }
        }
        val totalWeight = weights.sum()
        var roll = Random.nextFloat() * totalWeight
        var selected = available.last()
        for (i in available.indices) {
            roll -= weights[i]
            if (roll <= 0) {
                selected = available[i]
                break
            }
        }

        val isNew = selected.emojiId !in _ownedEmojis.value

        return LootboxReward(
            type = LootboxRewardType.EMOJI,
            id = selected.emojiId,
            displayName = if (isNew) selected.displayName else "${selected.displayName} (Duplikat)",
            rarity = selected.rarity,
            icon = selected.emoji
        )
    }

    // Persistence
    private fun loadLootboxes(): List<OwnedLootbox> {
        val json = prefs.getString("lootboxes", null) ?: return emptyList()
        return try {
            json.split("|").filter { it.isNotEmpty() }.map { entry ->
                val parts = entry.split(",")
                OwnedLootbox(
                    id = parts[0],
                    rarity = LootboxRarity.valueOf(parts[1]),
                    earnedAt = parts[2].toLong(),
                    source = parts.getOrElse(3) { "" }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveLootboxes(boxes: List<OwnedLootbox>) {
        val json = boxes.joinToString("|") { "${it.id},${it.rarity.name},${it.earnedAt},${it.source}" }
        prefs.edit().putString("lootboxes", json).apply()
    }

    private fun loadOwnedEmojis(): Set<String> {
        return prefs.getStringSet("owned_emojis", emptySet()) ?: emptySet()
    }

    private fun saveOwnedEmojis(emojis: Set<String>) {
        prefs.edit().putStringSet("owned_emojis", emojis).apply()
    }

    private fun loadSelectedEmoji(): String? {
        return prefs.getString("selected_emoji", null)
    }

    companion object {
        @Volatile
        private var instance: LootboxRepository? = null

        fun getInstance(context: Context): LootboxRepository {
            return instance ?: synchronized(this) {
                instance ?: LootboxRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
