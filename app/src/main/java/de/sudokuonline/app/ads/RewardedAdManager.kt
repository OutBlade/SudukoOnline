package de.sudokuonline.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for Rewarded Ads - Watch ad to get extra hints
 */
class RewardedAdManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "RewardedAdManager"
        
        // Test Ad Unit ID (replace with real one for production)
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        
        @Volatile
        private var instance: RewardedAdManager? = null
        
        fun getInstance(context: Context): RewardedAdManager {
            return instance ?: synchronized(this) {
                instance ?: RewardedAdManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false
    
    private val _adState = MutableStateFlow(AdState.NOT_LOADED)
    val adState: StateFlow<AdState> = _adState.asStateFlow()
    
    private val _lastReward = MutableStateFlow<AdReward?>(null)
    val lastReward: StateFlow<AdReward?> = _lastReward.asStateFlow()
    
    enum class AdState {
        NOT_LOADED,
        LOADING,
        READY,
        SHOWING,
        EARNED_REWARD,
        FAILED,
        CLOSED
    }
    
    data class AdReward(
        val type: String,
        val amount: Int
    )
    
    /**
     * Initialize Mobile Ads SDK
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: $initializationStatus")
            isInitialized = true
            loadAd(context)
        }
    }
    
    /**
     * Load a rewarded ad
     */
    fun loadAd(context: Context) {
        if (_adState.value == AdState.LOADING || _adState.value == AdState.READY) {
            return
        }
        
        _adState.value = AdState.LOADING
        
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    _adState.value = AdState.FAILED
                }
                
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully")
                    rewardedAd = ad
                    _adState.value = AdState.READY
                    setupFullScreenCallback()
                }
            }
        )
    }
    
    private fun setupFullScreenCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                rewardedAd = null
                _adState.value = AdState.CLOSED
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                rewardedAd = null
                _adState.value = AdState.FAILED
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed")
                _adState.value = AdState.SHOWING
            }
        }
    }
    
    /**
     * Show the rewarded ad
     * @param activity The activity context
     * @param onRewardEarned Callback when user earns reward
     * @param onAdClosed Callback when ad is closed (with or without reward)
     */
    fun showAd(
        activity: Activity,
        onRewardEarned: (AdReward) -> Unit,
        onAdClosed: () -> Unit
    ) {
        val ad = rewardedAd
        
        if (ad == null) {
            Log.w(TAG, "Ad not ready, loading new one")
            _adState.value = AdState.NOT_LOADED
            loadAd(activity)
            return
        }
        
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed")
                rewardedAd = null
                _adState.value = AdState.CLOSED
                onAdClosed()
                // Pre-load next ad
                loadAd(activity)
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Ad failed to show: ${adError.message}")
                rewardedAd = null
                _adState.value = AdState.FAILED
                onAdClosed()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed")
                _adState.value = AdState.SHOWING
            }
        }
        
        ad.show(activity) { rewardItem ->
            val reward = AdReward(
                type = rewardItem.type,
                amount = rewardItem.amount
            )
            Log.d(TAG, "User earned reward: ${reward.amount} ${reward.type}")
            _lastReward.value = reward
            _adState.value = AdState.EARNED_REWARD
            onRewardEarned(reward)
        }
    }
    
    /**
     * Check if ad is ready to show
     */
    fun isAdReady(): Boolean = rewardedAd != null && _adState.value == AdState.READY
    
    /**
     * Clear last reward
     */
    fun clearLastReward() {
        _lastReward.value = null
    }
}
