/*
 * Copyright 2013 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.core.impl.score.buildin.hardsoftdouble;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.buildin.hardsoftdouble.HardSoftDoubleScore;
import org.optaplanner.core.api.score.buildin.hardsoftdouble.HardSoftDoubleScoreHolder;
import org.optaplanner.core.api.score.constraint.primdouble.DoubleConstraintMatch;
import org.optaplanner.core.api.score.holder.ScoreHolder;
import org.optaplanner.core.impl.score.definition.AbstractFeasibilityScoreDefinition;
import org.optaplanner.core.impl.score.definition.AbstractScoreDefinition;
import org.optaplanner.core.impl.score.trend.InitializingScoreTrend;
import org.optaplanner.core.impl.score.trend.InitializingScoreTrendLevel;

public class HardSoftDoubleScoreDefinition extends AbstractFeasibilityScoreDefinition<HardSoftDoubleScore> {

    private double hardScoreTimeGradientWeight = 0.75; // TODO this is a guess

    public double getHardScoreTimeGradientWeight() {
        return hardScoreTimeGradientWeight;
    }

    /**
     * It's recommended to use a number which can be exactly represented as a double,
     * such as 0.5, 0.25, 0.75, 0.125, ... but not 0.1, 0.2, ...
     * @param hardScoreTimeGradientWeight 0.0 <= hardScoreTimeGradientWeight <= 1.0
     */
    public void setHardScoreTimeGradientWeight(double hardScoreTimeGradientWeight) {
        this.hardScoreTimeGradientWeight = hardScoreTimeGradientWeight;
        if (hardScoreTimeGradientWeight < 0.0 || hardScoreTimeGradientWeight > 1.0) {
            throw new IllegalArgumentException("Property hardScoreTimeGradientWeight (" + hardScoreTimeGradientWeight
                    + ") must be greater or equal to 0.0 and smaller or equal to 1.0.");
        }
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public int getLevelCount() {
        return 2;
    }

    @Override
    public int getFeasibleLevelCount() {
        return 1;
    }

    public Class<HardSoftDoubleScore> getScoreClass() {
        return HardSoftDoubleScore.class;
    }

    public HardSoftDoubleScore parseScore(String scoreString) {
        return HardSoftDoubleScore.parseScore(scoreString);
    }

    public double calculateTimeGradient(HardSoftDoubleScore startScore, HardSoftDoubleScore endScore,
            HardSoftDoubleScore score) {
        if (score.compareTo(endScore) > 0) {
            return 1.0;
        } else if (score.compareTo(startScore) < 0) {
            return 0.0;
        }
        double timeGradient = 0.0;
        double softScoreTimeGradientWeight = 1.0 - hardScoreTimeGradientWeight;
        if (startScore.getHardScore() == endScore.getHardScore()) {
            timeGradient += hardScoreTimeGradientWeight;
        } else {
            double hardScoreTotal = endScore.getHardScore() - startScore.getHardScore();
            double hardScoreDelta = score.getHardScore() - startScore.getHardScore();
            double hardTimeGradient = hardScoreDelta / hardScoreTotal;
            timeGradient += hardTimeGradient * hardScoreTimeGradientWeight;
        }
        if (score.getSoftScore() >= endScore.getSoftScore()) {
            timeGradient += softScoreTimeGradientWeight;
        } else if (score.getSoftScore() <= startScore.getSoftScore()) {
            // No change: timeGradient += 0.0
        } else {
            double softScoreTotal = endScore.getSoftScore() - startScore.getSoftScore();
            double softScoreDelta = score.getSoftScore() - startScore.getSoftScore();
            double softTimeGradient = softScoreDelta / softScoreTotal;
            timeGradient += softTimeGradient * softScoreTimeGradientWeight;
        }
        return timeGradient;
    }

    public HardSoftDoubleScoreHolder buildScoreHolder(boolean constraintMatchEnabled) {
        return new HardSoftDoubleScoreHolder(constraintMatchEnabled);
    }

    public HardSoftDoubleScore buildOptimisticBound(InitializingScoreTrend initializingScoreTrend, HardSoftDoubleScore score) {
        InitializingScoreTrendLevel[] trendLevels = initializingScoreTrend.getTrendLevels();
        return HardSoftDoubleScore.valueOf(
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_DOWN ? score.getHardScore() : Double.POSITIVE_INFINITY,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_DOWN ? score.getSoftScore() : Double.POSITIVE_INFINITY);
    }

    public HardSoftDoubleScore buildPessimisticBound(InitializingScoreTrend initializingScoreTrend, HardSoftDoubleScore score) {
        InitializingScoreTrendLevel[] trendLevels = initializingScoreTrend.getTrendLevels();
        return HardSoftDoubleScore.valueOf(
                trendLevels[0] == InitializingScoreTrendLevel.ONLY_UP ? score.getHardScore() : Double.NEGATIVE_INFINITY,
                trendLevels[1] == InitializingScoreTrendLevel.ONLY_UP ? score.getSoftScore() : Double.NEGATIVE_INFINITY);
    }

    public double calculateFeasibilityTimeGradient(HardSoftDoubleScore startScore, HardSoftDoubleScore score) {
        if (score.getHardScore() <= startScore.getHardScore()) {
            return 0.0;
        }
        double timeGradient = (startScore.getHardScore() - score.getHardScore()) / startScore.getHardScore();
        return Math.min(timeGradient, 1.0);
    }

}
