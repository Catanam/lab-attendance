package com.example.attendance.security.hmac;

import com.example.attendance.domain.device.Device;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

public class HmacAuthFilter extends OncePerRequestFilter {
    private static final long MAX_SKEW_SECONDS = 60L;

    private static final long MAX_BODY_BYTES = 64L * 1024L;

    private final DeviceCredentialResolver credentialResolver;
    private final NonceCache nonceCache;

    public HmacAuthFilter(DeviceCredentialResolver credentialResolver, NonceCache nonceCache) {
        this.credentialResolver = credentialResolver;
        this.nonceCache = nonceCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        long declaredLen = request.getContentLengthLong();
        if (declaredLen > MAX_BODY_BYTES) {
            rejectPayloadTooLarge(response);
            return;
        }

        byte[] cachedBody = readCapped(request.getInputStream());
        if (cachedBody == null) {
            rejectPayloadTooLarge(response);
            return;
        }
        HttpServletRequest wrapped = new CachedBodyRequest(request, cachedBody);

        String deviceId  = wrapped.getHeader("X-Device-ID");
        String timestamp = wrapped.getHeader("X-Timestamp");
        String nonce     = wrapped.getHeader("X-Nonce");
        String hmacHex   = wrapped.getHeader("X-HMAC");

        if (deviceId == null || timestamp == null || nonce == null || hmacHex == null) {
            rejectUnauthorized(response, "Missing HMAC headers");
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            rejectUnauthorized(response, "Invalid timestamp");
            return;
        }
        long skew = Math.abs(Instant.now().getEpochSecond() - ts);
        if (skew > MAX_SKEW_SECONDS) {
            rejectUnauthorized(response, "Timestamp skew too large");
            return;
        }

        if (nonceCache.isReplayed(nonce)) {
            rejectUnauthorized(response, "Replayed nonce");
            return;
        }

        Optional<Device> deviceOpt = credentialResolver.resolveActive(deviceId);
        if (deviceOpt.isEmpty()) {
            rejectUnauthorized(response, "Unknown or revoked device");
            return;
        }
        Device device = deviceOpt.get();

        String bodySha256 = sha256Hex(cachedBody);

        String hmacInput = timestamp + ":" + deviceId + ":" + nonce + ":" + bodySha256;
        String expectedHmac = hmacSha256Hex(device.getSecretHash(), hmacInput);

        if (!MessageDigest.isEqual(
                expectedHmac.getBytes(StandardCharsets.UTF_8),
                hmacHex.getBytes(StandardCharsets.UTF_8))) {
            rejectUnauthorized(response, "HMAC mismatch");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                device.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(wrapped, response);
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String hmacSha256Hex(String secret, String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private static void rejectUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
        response.flushBuffer();
    }

    private static void rejectPayloadTooLarge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Body too large\"}");
        response.flushBuffer();
    }

    private static byte[] readCapped(java.io.InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        long total = 0;
        while ((n = in.read(chunk)) != -1) {
            total += n;
            if (total > MAX_BODY_BYTES) return null;
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;
        CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }
        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override public int  read()         { return bais.read(); }
                @Override public boolean isFinished(){ return bais.available() == 0; }
                @Override public boolean isReady()   { return true; }
                @Override public void setReadListener(ReadListener listener) {}
            };
        }
        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
