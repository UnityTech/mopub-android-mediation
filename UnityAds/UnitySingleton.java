package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.MoPub;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.IUnitySdkListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.metadata.MediationMetaData;
import com.unity3d.ads.metadata.MetaData;
import com.unity3d.services.core.properties.SdkProperties;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class UnitySingleton implements IUnitySdkListener {
	private static UnitySingleton mInstance;

	/**
	 * Returns the global instance of the UnitySingleton, creating it if it does not exist yet.
	 * @return
	 */
	public synchronized static UnitySingleton getInstance() {
		if (mInstance == null) {
			mInstance = new UnitySingleton();
		}
		return mInstance;
	}

	/**
	 * State of the UnityAds SDK.
	 */
	public enum UnitySdkInitState {
		NOT_INITIALIZED,
		INITIALIZING,
		INITIALIZED,
		ERROR
	}

	private static final String GAME_ID_KEY = "gameId";

	// A queue of sdk listeners to be notified
	private Set<WeakReference<IUnitySdkListener>> mSdkListeners =
			Collections.synchronizedSet(new HashSet<WeakReference<IUnitySdkListener>>());

	// Boolean to determine if the SDK is initialized or not. Will be set when SDK is finished
	// initializing.
	private UnitySdkInitState mInitState = UnitySdkInitState.NOT_INITIALIZED;

	// If the SDK initialized with an exception, we will record it to prevent reinitialization.
	// Assume this will be non-null if mInitState is InitState.ERROR
	private Exception mInitException;

	/**
	 * Hidden constructor.
	 */
	private UnitySingleton() { /**/ }

	/**
	 * Initializes UnityAds.
	 *
	 * @param context The android context passed from MoPub.
	 * @param serverExtras The server extras passed into the event class.
     * @param sdkListener The SDK lifecycle listener, will be used to notify when SDK initializes or
	 *                    fails to initialize.
	 * @return True if initialization was called, false if it was already initialized.
	 */
	public boolean initUnityAds(Context context,
	                         Map<String, String> serverExtras,
	                         IUnitySdkListener sdkListener) {
		if (isState(UnitySdkInitState.INITIALIZED)) {
			// if already initialized, then just notify the SDK listener.
			sdkListener.onSdkInitialized();
			return false;
		} else if (isState(UnitySdkInitState.ERROR)) {
			// An error occurred while initializing, so just notify the SDK listener.
			sdkListener.onSdkInitializationFailed(mInitException);
			return false;
		} else if (isState(UnitySdkInitState.INITIALIZING)) {
			// If we are already initialized, we will just append the SDK listener to the set.
			mSdkListeners.add(new WeakReference<>(sdkListener));
			return false;
		} else {
			// Actually do the initialization.
			mSdkListeners.add(new WeakReference<>(sdkListener));
			if (!doInit(context, serverExtras)) {
				// An error occurred within the sanity check of UnityAds. Assume that the state is now errored.
				notifyListenersSdkInitializationDidFail();
			}
		}
		return true;
	}

	/**
	 * Returns if the UnitySingleton is in a given state
	 * @param testState The state to test against
	 * @return True if in the given state, else false.
	 */
	private boolean isState(UnitySdkInitState testState) {
		return mInitState.equals(testState);
	}

	/**
	 * Actually does the initializing UnityAds.
	 * @param context
	 * @param serverExtras
	 *
	 * @return True if initialization was started, else false for error.
	 */
	private boolean doInit(Context context, Map<String, String> serverExtras) {
		if (UnityAds.isInitialized()) {
			// In theory, this should never occur, but let's sanity check it anyway.
			return true;
		}

		mInitState = UnitySdkInitState.INITIALIZING;

		if (!UnityAds.isSupported()) {
			setErrorState(new IllegalStateException("Platform is not supported for Unity Ads"));
			return false;
		}

		// Validate the game ID
		String gameId = serverExtras.get(GAME_ID_KEY);
		if (gameId == null || gameId.isEmpty()) {
			setErrorState(new IllegalArgumentException(String.format(
					"Server extras did not contain key for: \"%s\"", GAME_ID_KEY)));
			return false;
		}

		// Validate the context
		if (context == null || !(context instanceof Activity)) {
			setErrorState(new IllegalArgumentException("Context was not a valid Activity instance"));
			return false;
		}

		setMediationMetadata(context);
		setGdprConsentMetadata(context);

		try {
			SdkProperties.setConfigUrl("http://10.1.82.108:8000/build/dev/config.json");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		UnityAds.setInitListener(this);
		UnityAds.initialize((Activity) context, gameId, new NullUnityAdsListener());
		return true;
	}

	@Override
	public void onSdkInitialized() {
		mInitState = UnitySdkInitState.INITIALIZED;

		Iterator<WeakReference<IUnitySdkListener>> it = mSdkListeners.iterator();
		while (it.hasNext()) {
			try {
				IUnitySdkListener listener = it.next().get();
				if (listener != null) {
					listener.onSdkInitialized();
				}
			} finally {
				it.remove();
			}
		}
	}

	@Override
	public void onSdkInitializationFailed(Exception e) {
		setErrorState(e);
		notifyListenersSdkInitializationDidFail();
	}

	private void notifyListenersSdkInitializationDidFail() {
		Iterator<WeakReference<IUnitySdkListener>> it = mSdkListeners.iterator();
		while (it.hasNext()) {
			try {
				IUnitySdkListener listener = it.next().get();
				if (listener != null) {
					listener.onSdkInitializationFailed(mInitException);
				}
			} finally {
				it.remove();
			}
		}
	}

	/**
	 * Sets the singleton into the errored state with the given message.
	 * @param e The exception to set as the init exception.
	 */
	private void setErrorState(Exception e) {
		mInitState = UnitySdkInitState.ERROR;
		mInitException = e;
	}

	/**
	 * Sets the GDPR consent metadata within UnityAds
	 * @param context Context used to initialize UnityAds.
	 */
	private void setGdprConsentMetadata(Context context) {
		// Pass the user consent from the MoPub SDK to Unity Ads as per GDPR
		PersonalInfoManager personalInfoManager = MoPub.getPersonalInformationManager();

		if (personalInfoManager != null) {
			ConsentStatus consentStatus = personalInfoManager.getPersonalInfoConsentStatus();

			if(consentStatus == ConsentStatus.EXPLICIT_YES || consentStatus == ConsentStatus.EXPLICIT_NO) {
				MetaData gdprMetaData = new MetaData(context);

				// Set if the user has explicitly said yes or no
				boolean doesConsent = consentStatus == ConsentStatus.EXPLICIT_YES;
				gdprMetaData.set("gdpr.consent", doesConsent);
				gdprMetaData.commit();
			}
		}
	}

	/**
	 * Sets the Mediation metadata within UnityAds
	 * @param context Context used to initialize UnityAds.
	 */
	private void setMediationMetadata(Context context) {
		MediationMetaData mediationMetaData = new MediationMetaData(context);
		mediationMetaData.setName("MoPub");
		mediationMetaData.setVersion(MoPub.SDK_VERSION);
		mediationMetaData.commit();
	}

	/**
	 * This class is needed for {@link UnityAds#initialize(Activity, String, IUnityAdsListener)},
	 * but is not actually used under mediation.
	 */
	private class NullUnityAdsListener implements IUnityAdsListener {
		@Override
		public void onUnityAdsReady(String s) { }

		@Override
		public void onUnityAdsStart(String s) { }

		@Override
		public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) { }

		@Override
		public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) { }
	}
}
