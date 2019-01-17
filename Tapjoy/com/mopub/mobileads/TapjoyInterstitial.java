// Copyright (C) 2015 by Tapjoy Inc.
//
// This file is part of the Tapjoy SDK.
//
// By using the Tapjoy SDK in your software, you agree to the terms of the Tapjoy SDK License Agreement.
//
// The Tapjoy SDK is bound by the Tapjoy SDK License Agreement and can be found here: https://www.tapjoy.com/sdk/license

package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Json;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyLog;

import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class TapjoyInterstitial extends CustomEventInterstitial implements TJPlacementListener {
    private static final String TAG = TapjoyInterstitial.class.getSimpleName();
    private static final String TJC_MOPUB_NETWORK_CONSTANT = "mopub";
    private static final String TJC_MOPUB_ADAPTER_VERSION_NUMBER = "4.1.0";

    // Configuration keys
    public static final String SDK_KEY = "sdkKey";
    public static final String DEBUG_ENABLED = "debugEnabled";
    public static final String PLACEMENT_NAME = "name";
    private static final String ADM_KEY = "adm";

    private TJPlacement tjPlacement;
    private CustomEventInterstitialListener mInterstitialListener;
    private Handler mHandler;

    static {
        TapjoyLog.i(TAG, "Class initialized with network adapter version " + TJC_MOPUB_ADAPTER_VERSION_NUMBER);
    }

    @Override
    protected void loadInterstitial(final Context context,
                                    CustomEventInterstitialListener customEventInterstitialListener,
                                    Map<String, Object> localExtras,
                                    Map<String, String> serverExtras) {
        MoPubLog.d("Requesting Tapjoy interstitial");

        mInterstitialListener = customEventInterstitialListener;
        mHandler = new Handler(Looper.getMainLooper());

        fetchMoPubGDPRSettings();

        final String placementName = serverExtras.get(PLACEMENT_NAME);
        if (TextUtils.isEmpty(placementName)) {
            MoPubLog.d("Tapjoy interstitial loaded with empty 'name' field. Request will fail.");
        }

        final String adm = serverExtras.get(ADM_KEY);

        boolean canRequestPlacement = true;
        if (!Tapjoy.isConnected()) {
            // Check if configuration data is available
            boolean enableDebug = Boolean.valueOf(serverExtras.get(DEBUG_ENABLED));
            Tapjoy.setDebugEnabled(enableDebug);

            String sdkKey = serverExtras.get(SDK_KEY);
            if (!TextUtils.isEmpty(sdkKey)) {
                MoPubLog.d("Connecting to Tapjoy via MoPub dashboard settings...");
                Tapjoy.connect(context, sdkKey, null, new TJConnectListener() {
                    @Override
                    public void onConnectSuccess() {
                        MoPubLog.d("Tapjoy connected successfully");
                        createPlacement(context, placementName, adm);
                    }

                    @Override
                    public void onConnectFailure() {
                        MoPubLog.d("Tapjoy connect failed");
                    }
                });

                // If sdkKey is present via MoPub dashboard, we only want to request placement
                // after auto-connect succeeds
                canRequestPlacement = false;
            } else {
                MoPubLog.d("Tapjoy interstitial is initialized with empty 'sdkKey'. You must call Tapjoy.connect()");
            }
        }

        if (canRequestPlacement) {
            createPlacement(context, placementName, adm);
        }
    }

    private void createPlacement(Context context, String placementName, final String adm) {
        tjPlacement = new TJPlacement(context, placementName, this);
        tjPlacement.setMediationName(TJC_MOPUB_NETWORK_CONSTANT);
        tjPlacement.setAdapterVersion(TJC_MOPUB_ADAPTER_VERSION_NUMBER);

        if (!TextUtils.isEmpty(adm)) {
            try {
                Map<String, String> auctionData = Json.jsonStringToMap(adm);
                tjPlacement.setAuctionData(new HashMap<>(auctionData));
            } catch (JSONException e) {
                MoPubLog.d("Unable to parse auction data.");
            }
        }

        tjPlacement.requestContent();
    }

    // Pass the user consent from the MoPub SDK to Tapjoy as per GDPR
    private void fetchMoPubGDPRSettings() {

        PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

        if (personalInfoManager != null) {
            Boolean gdprApplies = personalInfoManager.gdprApplies();

            if (gdprApplies != null) {
                Tapjoy.subjectToGDPR(gdprApplies);

                if (gdprApplies) {
                    String userConsented = MoPub.canCollectPersonalInformation() ? "1" : "0";

                    Tapjoy.setUserConsent(userConsented);
                } else {
                    Tapjoy.setUserConsent("-1");
                }
            }
        }
    }

    @Override
    protected void onInvalidate() {
        // No custom cleanup to do here.
    }

    @Override
    protected void showInterstitial() {
        MoPubLog.d("Tapjoy interstitial will be shown");
        tjPlacement.showContent();
    }

    // Tapjoy

    @Override
    public void onRequestSuccess(final TJPlacement placement) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (placement.isContentAvailable()) {
                    MoPubLog.d("Tapjoy interstitial request successful");
                    mInterstitialListener.onInterstitialLoaded();
                } else {
                    MoPubLog.d("No Tapjoy interstitials available");
                    mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
                }
            }
        });
    }

    @Override
    public void onRequestFailure(TJPlacement placement, TJError error) {
        MoPubLog.d("Tapjoy interstitial request failed");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
            }
        });
    }

    @Override
    public void onContentShow(TJPlacement placement) {
        MoPubLog.d("Tapjoy interstitial shown");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialShown();
            }
        });
    }

    @Override
    public void onContentDismiss(TJPlacement placement) {
        MoPubLog.d("Tapjoy interstitial dismissed");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mInterstitialListener.onInterstitialDismissed();
            }
        });
    }

    @Override
    public void onContentReady(TJPlacement placement) {
    }

    @Override
    public void onPurchaseRequest(TJPlacement placement, TJActionRequest request,
                                  String productId) {
    }

    @Override
    public void onRewardRequest(TJPlacement placement, TJActionRequest request, String itemId,
                                int quantity) {
    }
}
