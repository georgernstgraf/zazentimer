package at.priv.graf.zazentimer.grpc;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import io.grpc.Status;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.concurrent.Executor;

public class JwtCallCredentials extends CallCredentials {

    private static final Metadata.Key<String> AUTHORIZATION_METADATA_KEY =
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    private final Key key;
    private final String issuer;
    private final String subject;
    private final long expirationMillis;

    public JwtCallCredentials(String issuer, String subject, long expirationMillis) {
        this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        this.issuer = issuer;
        this.subject = subject;
        this.expirationMillis = expirationMillis;
    }

    public JwtCallCredentials(Key key, String issuer, String subject, long expirationMillis) {
        this.key = key;
        this.issuer = issuer;
        this.subject = subject;
        this.expirationMillis = expirationMillis;
    }

    private String generateToken() {
        long nowMillis = System.currentTimeMillis();
        return Jwts.builder()
                .setIssuer(issuer)
                .setSubject(subject)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(nowMillis + expirationMillis))
                .signWith(key)
                .compact();
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
        appExecutor.execute(() -> {
            try {
                String token = generateToken();
                Metadata headers = new Metadata();
                headers.put(AUTHORIZATION_METADATA_KEY, "Bearer " + token);
                applier.apply(headers);
            } catch (Throwable e) {
                applier.fail(Status.UNAUTHENTICATED.withCause(e).withDescription("Failed to generate JWT token"));
            }
        });
    }

    @Override
    public void thisUsesUnstableApi() {
        // Required for gRPC CallCredentials
    }
}
