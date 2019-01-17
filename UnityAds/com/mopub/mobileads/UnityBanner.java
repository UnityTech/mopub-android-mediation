package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.logging.MoPubLog;
import com.unity3d.ads.IUnitySdkListener;
import com.unity3d.services.ads.BannerAd;

import java.util.Map;

public class UnityBanner extends CustomEventBanner implements IUnitySdkListener, BannerAd.Listener {
	private String mPlacementId;
	private BannerAd mBannerAd;
	private CustomEventBannerListener mCustomEventBannerListener;
	private boolean mImpressionSent;

	@Override
	protected void loadBanner(Context context,
	                          CustomEventBannerListener customEventBannerListener,
	                          Map<String, Object> localExtras,
	                          Map<String, String> serverExtras) {
		mCustomEventBannerListener = customEventBannerListener;
		mPlacementId = UnityMediationUtilities.getPlacementIdForServerExtras(serverExtras, mPlacementId);

		mBannerAd = new BannerAd((Activity)context, mPlacementId);
		mBannerAd.setListener(this);

		UnitySingleton.getInstance().initUnityAds(context, serverExtras, this);
	}

	@Override
	protected void onInvalidate() {
		if (mBannerAd != null) {
			mBannerAd.destroy();
			mBannerAd = null;
		}
	}

	@Override
	public void onSdkInitialized() {
		if (mBannerAd != null) {
			mBannerAd.load();
		}
	}

	@Override
	public void onSdkInitializationFailed(Exception e) {
		if (mCustomEventBannerListener != null) {
			mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public void onAdLoaded() {
		if (mCustomEventBannerListener != null) {
			if (mBannerAd != null) {
				View view = mBannerAd.getView();
				if (view != null) {
					mCustomEventBannerListener.onBannerLoaded(view);
				} else {
					MoPubLog.e("No view was loaded for banner.");
					mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
				}
			}
		}
	}

	@Override
	public void onAdFailedToLoad(Exception e) {
		if (mCustomEventBannerListener != null) {
			mCustomEventBannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public void onAdOpening() {
		if (mCustomEventBannerListener != null && !mImpressionSent) {
			mImpressionSent = true;
			mCustomEventBannerListener.onBannerImpression();
		}
	}

	@Override
	public void onAdClosing() {

	}

	@Override
	public void onAdClick() {
		if (mCustomEventBannerListener != null) {
			mCustomEventBannerListener.onBannerClicked();
		}
	}

	@Override
	public void onAdLeavingApplication() {

	}
}
