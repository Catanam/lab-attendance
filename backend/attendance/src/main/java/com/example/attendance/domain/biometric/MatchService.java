package com.example.attendance.domain.biometric;

import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class MatchService {
    private final double matchThreshold;

    private final MatchCache matchCache;
    private final FingerprintImageDecoder imageDecoder;

    public MatchService(MatchCache matchCache,
                        FingerprintImageDecoder imageDecoder,
                        @Value("${biometric.match-threshold:40.0}") double matchThreshold) {
        this.matchCache = matchCache;
        this.imageDecoder = imageDecoder;
        this.matchThreshold = matchThreshold;
    }

    public record MatchResult(Optional<UUID> studentId, double score) {}

    public MatchResult match(byte[] raw4bitImage) throws Exception {
        FingerprintTemplate captured = new FingerprintTemplate(imageDecoder.decode(raw4bitImage));
        FingerprintMatcher matcher = new FingerprintMatcher(captured);

        UUID bestStudent = null;
        double bestScore = 0.0;

        for (Map.Entry<UUID, List<FingerprintTemplate>> entry : matchCache.all().entrySet()) {
            for (FingerprintTemplate stored : entry.getValue()) {
                double score = matcher.match(stored);
                if (score > bestScore) {
                    bestScore = score;
                    bestStudent = entry.getKey();
                }
            }
        }

        if (bestStudent != null && bestScore >= matchThreshold) {
            return new MatchResult(Optional.of(bestStudent), bestScore);
        }
        return new MatchResult(Optional.empty(), bestScore);
    }
}
