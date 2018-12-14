package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnitySdkListener;
import com.unity3d.ads.mediation.IUnityAdsExtendedListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.ads.PlacementAd;

import java.util.Map;

public class UnityRewardedVideo extends CustomEventRewardedVideo
        implements IUnitySdkListener, PlacementAd.Listener {

    private Context mContext;
    private String mPlacementId;
    private PlacementAd mPlacementAd;
    private LifecycleListener mLifecycleListener = new BaseLifecycleListener();

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return mLifecycleListener;
    }

    @Override
    public boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                         @NonNull final Map<String, Object> localExtras,
                                         @NonNull final Map<String, String> serverExtras) {

        mPlacementId = UnityMediationUtilities.getPlacementIdForServerExtras(serverExtras, mPlacementId);
        mContext = launcherActivity;
        mPlacementAd = new PlacementAd(mPlacementId);
        mPlacementAd.setListener(this);

        UnitySingleton unitySingleton = UnitySingleton.getInstance();
        if (unitySingleton.isState(UnitySingleton.UnitySdkInitState.NOT_INITIALIZED)) {
            return false;
        } else {
            unitySingleton.initUnityAds(launcherActivity, serverExtras, this);
            return true;
        }
    }

    @Override
    protected void loadWithSdkInitialized(@NonNull Activity activity, @NonNull Map<String, Object> localExtras, @NonNull Map<String, String> serverExtras) { }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return mPlacementId;
    }

    @Override
    public boolean hasVideoAvailable() {
    	if (mPlacementAd != null) {
    	    return mPlacementAd.isLoaded();
        }
        return false;
    }

    @Override
    public void showVideo() {
    	if (mPlacementAd != null) {
    	    mPlacementAd.show((Activity) mContext);
        }
    }

    @Override
    protected void onInvalidate() {
    	if (mPlacementAd != null) {
    	    mPlacementAd.destroy();
        }
    }

    @Override
    public void onSdkInitialized() {
        if (mPlacementAd != null) {
            mPlacementAd.load();
        }
    }

    @Override
    public void onSdkInitializationFailed(Exception e) {
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, mPlacementId, MoPubErrorCode.INTERNAL_ERROR);
    }

    @Override
    public void onAdLoaded() {
        MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(UnityRewardedVideo.class, mPlacementId);
    }

    @Override
    public void onAdFailedToLoad(Exception e) {
        MoPubRewardedVideoManager.onRewardedVideoLoadFailure(UnityRewardedVideo.class, mPlacementId, MoPubErrorCode.INTERNAL_ERROR);
    }

    @Override
    public void onAdClosed(UnityAds.FinishState finishState) {
        if (finishState == UnityAds.FinishState.ERROR) {
            MoPubRewardedVideoManager.onRewardedVideoPlaybackError(
                    UnityRewardedVideo.class,
                    mPlacementId,
                    MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
            MoPubLog.d(String.format("Unity rewarded video encountered a playback error for placement %s", mPlacementId));
        } else if (finishState == UnityAds.FinishState.COMPLETED) {
            MoPubRewardedVideoManager.onRewardedVideoCompleted(
                    UnityRewardedVideo.class,
                    mPlacementId,
                    MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.NO_REWARD_AMOUNT));
            MoPubLog.d(String.format("Unity rewarded video completed for placement %s", mPlacementId));
        } else if (finishState == UnityAds.FinishState.SKIPPED) {
            MoPubLog.d("Unity ad was skipped, no reward will be given.");
        }
        MoPubRewardedVideoManager.onRewardedVideoClosed(UnityRewardedVideo.class, mPlacementId);
    }
}
