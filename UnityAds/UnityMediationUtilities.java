package com.mopub.mobileads;

import java.util.Map;

public class UnityMediationUtilities {
	private static final String ZONE_ID_KEY = "zoneId";
	private static final String PLACEMENT_ID_KEY = "placementId";

	static String getPlacementIdForServerExtras(Map<String, String> serverExtras, String defaultPlacementId) {
		String placementId = null;
		if (serverExtras.containsKey(PLACEMENT_ID_KEY)) {
			placementId = serverExtras.get(PLACEMENT_ID_KEY);
		} else if (serverExtras.containsKey(ZONE_ID_KEY)) {
			placementId = serverExtras.get(ZONE_ID_KEY);
		}
		if (placementId == null || placementId.isEmpty()) {
			return defaultPlacementId;
		}
		return placementId;
	}
}
