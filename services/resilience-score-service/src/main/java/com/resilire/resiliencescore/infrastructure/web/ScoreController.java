package com.resilire.resiliencescore.infrastructure.web;

import com.resilire.resiliencescore.application.ScoringService;
import com.resilire.resiliencescore.domain.score.ForecastResult;
import com.resilire.resiliencescore.domain.score.ItineraryFeatures;
import com.resilire.resiliencescore.domain.score.ResilienceScore;
import com.resilire.resiliencescore.domain.score.RouteForecaster;
import com.resilire.resiliencescore.domain.score.ScoreFactor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound adapter for the point-of-sale scoring path (architecture.md §5.1).
 * Booking calls this while presenting offers; the response carries the 0–100
 * score, its band, the explaining factors, and the {@code source} flag so a
 * degraded score is visibly distinguished from a live model score.
 */
@RestController
@RequestMapping("/scores")
public class ScoreController {

    private final ScoringService scoring;

    public ScoreController(ScoringService scoring) {
        this.scoring = scoring;
    }

    @PostMapping
    public ScoreView score(@Valid @RequestBody ScoreRequest request) {
        return ScoreView.of(scoring.scoreCandidate(request.toFeatures()));
    }

    @GetMapping("/forecast")
    public ForecastView forecast(@RequestParam @NotBlank String origin,
                                 @RequestParam @NotBlank String destination,
                                 @RequestParam @NotBlank String carrier) {
        return ForecastView.of(RouteForecaster.forecast(origin, destination, carrier));
    }

    public record ScoreRequest(
            @NotBlank String origin,
            @NotBlank String destination,
            @NotBlank String marketingCarrier,
            @NotNull Instant departure,
            @PositiveOrZero int connections,
            boolean separateTickets) {

        ItineraryFeatures toFeatures() {
            return new ItineraryFeatures(origin, destination, marketingCarrier, departure,
                    connections, separateTickets);
        }
    }

    public record ScoreView(int value, String band, String source, String modelVersion,
                            Instant computedAt, List<FactorView> factors) {
        static ScoreView of(ResilienceScore s) {
            return new ScoreView(s.value(), s.band().name(), s.source().name(), s.modelVersion(),
                    s.computedAt(), s.factors().stream().map(FactorView::of).toList());
        }
    }

    public record FactorView(String code, String description, double weight) {
        static FactorView of(ScoreFactor f) {
            return new FactorView(f.code(), f.description(), f.weight());
        }
    }

    public record ForecastView(String origin, String destination, String carrier,
                               double disruptionProbability, double otpRate, double confidence) {
        static ForecastView of(ForecastResult r) {
            return new ForecastView(r.origin(), r.destination(), r.carrier(),
                    r.disruptionProbability(), r.otpRate(), r.confidence());
        }
    }
}
