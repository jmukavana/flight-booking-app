package com.resilire.resiliencescore.domain.score;

import java.util.Objects;

/**
 * One explainable contributor to a score (the "explainable score" of SDD §3 /
 * driver 3). {@code weight} is the signed contribution: negative pushes the score
 * down (raises risk), positive lifts it. Value object.
 */
public record ScoreFactor(String code, String description, double weight) {

    public ScoreFactor {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(description, "description");
    }
}
