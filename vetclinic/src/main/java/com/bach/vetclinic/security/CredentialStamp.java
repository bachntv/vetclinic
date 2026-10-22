package com.bach.vetclinic.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Invalidates issued tokens after password resets without exposing the password hash. */
public final class CredentialStamp {
    private CredentialStamp() {}
    public static String of(String hash) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(hash.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
