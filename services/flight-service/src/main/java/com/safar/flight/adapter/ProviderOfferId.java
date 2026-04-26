package com.safar.flight.adapter;

/**
 * Offers carry a {@code PROVIDER:nativeId} prefix in their {@code offerId} so
 * a subsequent book() call can be routed back to the originating adapter
 * without the client having to remember which provider produced which offer.
 */
public final class ProviderOfferId {

    private static final String SEP = ":";

    private ProviderOfferId() {}

    public static String encode(FlightProvider provider, String nativeId) {
        return provider.name() + SEP + nativeId;
    }

    public static FlightProvider provider(String prefixed) {
        int i = indexOfSep(prefixed);
        try {
            return FlightProvider.valueOf(prefixed.substring(0, i));
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed provider offer id: " + prefixed);
        }
    }

    public static String nativeId(String prefixed) {
        return prefixed.substring(indexOfSep(prefixed) + 1);
    }

    private static int indexOfSep(String prefixed) {
        if (prefixed == null) throw new IllegalArgumentException("offerId is null");
        int i = prefixed.indexOf(SEP);
        if (i <= 0 || i == prefixed.length() - 1) {
            throw new IllegalArgumentException(
                    "Malformed provider offer id (expected PROVIDER:nativeId): " + prefixed);
        }
        return i;
    }
}
