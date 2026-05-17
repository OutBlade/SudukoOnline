package de.sudokuonline.app.data.model

enum class LootboxRarity(
    val displayName: String,
    val price: Int,
    val colorValue: Long,
    val chestEmoji: String
) {
    BRONZE("Bronze", 200, 0xFFCD7F32, "\uD83D\uDCE6"),
    SILVER("Silber", 500, 0xFFC0C0C0, "\uD83D\uDCE6"),
    GOLD("Gold", 1000, 0xFFFFD700, "\uD83D\uDCE6"),
    LEGENDARY("Legendär", 2500, 0xFFE040FB, "\uD83D\uDCE6");

    val rewardCount: Int get() = when (this) {
        BRONZE -> 3
        SILVER -> 4
        GOLD -> 5
        LEGENDARY -> 6
    }
}

enum class LootboxRewardType { COINS, THEME, EMOJI }

enum class RewardRarity(val displayName: String, val colorValue: Long) {
    COMMON("Gewöhnlich", 0xFF9E9E9E),
    RARE("Selten", 0xFF2196F3),
    EPIC("Episch", 0xFF9C27B0),
    LEGENDARY("Legendär", 0xFFFFD700)
}

data class LootboxReward(
    val type: LootboxRewardType,
    val id: String,
    val displayName: String,
    val amount: Int = 0,
    val rarity: RewardRarity = RewardRarity.COMMON,
    val icon: String = ""
)

data class OwnedLootbox(
    val id: String,
    val rarity: LootboxRarity,
    val earnedAt: Long = System.currentTimeMillis(),
    val source: String = ""
)

enum class ProfileEmoji(
    val emojiId: String,
    val emoji: String,
    val displayName: String,
    val rarity: RewardRarity
) {
    FIRE("fire", "\uD83D\uDD25", "Feuer", RewardRarity.COMMON),
    STAR("star", "\u2B50", "Stern", RewardRarity.COMMON),
    TROPHY("trophy", "\uD83C\uDFC6", "Pokal", RewardRarity.COMMON),
    CROWN("crown", "\uD83D\uDC51", "Krone", RewardRarity.RARE),
    DIAMOND("diamond", "\uD83D\uDC8E", "Diamant", RewardRarity.RARE),
    SPARKLES("sparkles", "\u2728", "Funken", RewardRarity.RARE),
    ROCKET("rocket", "\uD83D\uDE80", "Rakete", RewardRarity.EPIC),
    LIGHTNING("lightning", "\u26A1", "Blitz", RewardRarity.EPIC),
    RAINBOW("rainbow", "\uD83C\uDF08", "Regenbogen", RewardRarity.EPIC),
    DRAGON("dragon", "\uD83D\uDC09", "Drache", RewardRarity.LEGENDARY),
    UNICORN("unicorn", "\uD83E\uDD84", "Einhorn", RewardRarity.LEGENDARY),
    ALIEN("alien", "\uD83D\uDC7D", "Alien", RewardRarity.LEGENDARY)
}
