package io.netnotes.system;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;

public interface SignatureVerifier {
    Ed25519PublicKeyParameters getSigningPublicKey();
}
