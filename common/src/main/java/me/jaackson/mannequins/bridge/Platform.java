package me.jaackson.mannequins.bridge;

/**
 * @author Jackson
 */
public final class Platform {

    // just so intellij will shut up
    public static <T> T safeAssertionError() {
        throw new AssertionError();
    }
}
