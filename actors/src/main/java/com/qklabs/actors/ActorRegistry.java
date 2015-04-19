package com.qklabs.actors;

import android.content.UriMatcher;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.SparseArray;

/**
 * ActorRegistry provides a Uri -> Actor class lookup mechanism.
 */
public class ActorRegistry {
    public static final String ACTOR_SCHEME = "actor";
    public static final String LOCAL_AUTHORITY = "local";


    // Important: Use non-negative integers since UriMatcher.NO_MATCH == -1.
    private static int sNextMatch = 0;
    private static SparseArray<Class<? extends Actor>> sMatchToClass = new SparseArray<>();
    private static UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private ActorRegistry() {}

    public static void register(String uriString, Class<? extends Actor> cls) {
        Uri uri = buildActorUri(uriString);
        register(uri, cls);
    }

    public static void register(Uri uri, Class<? extends Actor> cls) {
        uri = buildActorUri(uri);
        throwIfRegistered(uri);

        int match = getNextMatch();
        sUriMatcher.addURI(uri.getAuthority(), uri.getPath(), match);
        sMatchToClass.put(match, cls);
    }

    /**
     * Returns the Actor class corresponding to the given Uri if one has been registered. If the
     * Uri has not been registered, null is returned.
     * @param uriString the requested uri
     */
    @Nullable
    public static Class<? extends Actor> lookup(String uriString) {
        Uri uri = buildActorUri(uriString);
        int match = sUriMatcher.match(uri);
        return sMatchToClass.get(match, null);
    }

    /**
     * Returns the Actor class corresponding to the given Uri if one has been registered. If the
     * Uri has not been registered, null is returned.
     * @param uri the requested uri
     */
    @Nullable
    public static Class<? extends Actor> lookup(Uri uri) {
        uri = buildActorUri(uri);
        int match = sUriMatcher.match(uri);
        return sMatchToClass.get(match, null);
    }

    /**
     * Clears the ActorRegistry.
     */
    public static void clear() {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatchToClass.clear();
        sNextMatch = 0;
    }

    /**
     * Throws if the given uri is already registered in the ActorRegistry.
     * @param uri uri to check
     */
    private static void throwIfRegistered(Uri uri) {
        if (sUriMatcher.match(uri) != UriMatcher.NO_MATCH) {
            String msg = "The uri " + uri + " has already been registered";
            throw new IllegalStateException(msg);
        }
    }

    @NonNull
    private static Uri buildActorUri(String uriString) {
        Uri uri = Uri.parse(uriString);
        return buildActorUri(uri);
    }

    @NonNull
    private static Uri buildActorUri(Uri uri) {
        if (isActorUri(uri)) {
            return uri;
        } else {
            return new Uri.Builder()
                    .authority(getAuthority(uri))
                    .scheme(ACTOR_SCHEME)
                    .path(uri.getPath())
                    .build();
        }
    }

    private static String getAuthority(Uri uri) {
        return TextUtils.isEmpty(uri.getAuthority())
                ? LOCAL_AUTHORITY
                : uri.getAuthority();
    }

    private static boolean isActorUri(Uri uri) {
        return !TextUtils.isEmpty(uri.getAuthority())
                && !TextUtils.isEmpty(uri.getScheme())
                && uri.getScheme().equals(ACTOR_SCHEME);
    }

    private static int getNextMatch() {
        return sNextMatch++;
    }
}
