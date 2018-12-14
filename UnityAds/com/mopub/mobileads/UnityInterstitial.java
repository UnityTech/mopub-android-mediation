package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.unity3d.ads.IUnitySdkListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.ads.PlacementAd;

import java.util.Map;

public class UnityInterstitial extends CustomEventInterstitial implements IUnitySdkListener, PlacementAd.Listener {

    private PlacementAd mPlacementAd;
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private Context mContext;
    private String mPlacementId = "video";

    @Override
    protected void loadInterstitial(Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {

        mPlacementId = UnityMediationUtilities.getPlacementIdForServerExtras(serverExtras, mPlacementId);
        mCustomEventInterstitialListener = customEventInterstitialListener;
        mContext = context;
        mPlacementAd = new PlacementAd(mPlacementId);
        mPlacementAd.setListener(this);

        UnitySingleton.getInstance().initUnityAds(context, serverExtras, this);
    }

    @Override
    protected void showInterstitial() {
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
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
        }
    }

    @Override
    public void onAdLoaded() {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }
    }

    @Override
    public void onAdFailedToLoad(Exception e) {
        if (mCustomEventInterstitialListener != null) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void onAdClosed(UnityAds.FinishState finishState) {
        if (finishState.equals(UnityAds.FinishState.ERROR)) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_PLAYBACK_ERROR);
        } else {
            if (mCustomEventInterstitialListener != null) {
                mCustomEventInterstitialListener.onInterstitialDismissed();
            }
        }
    }
}
