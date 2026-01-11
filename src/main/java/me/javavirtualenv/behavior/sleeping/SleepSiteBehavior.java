package me.javavirtualenv.behavior.sleeping;

/**
 * Behavior for selecting optimal sleep sites.
 * Considers covered locations, elevated positions, and threat avoidance.
 * Refactored to be testable without Minecraft dependencies.
 */
public class SleepSiteBehavior {

    private final SleepingConfig config;
    private SleepBlockPos previousSite = null;
    private int ticksSinceSiteCheck = 0;

    public SleepSiteBehavior(SleepingConfig config) {
        this.config = config;
    }

    /**
     * Finds a suitable sleep site near the given position.
     * Considers covered locations, elevation, and avoids hostile mobs.
     * Returns a site within maxSleepSiteDistance of the current position.
     */
    public SleepBlockPos findSleepSite(SleepContext context) {
        SleepBlockPos currentPos = context.getBlockPos();
        int searchRadius = config.getMaxSleepSiteDistance();

        SleepBlockPos bestSite = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        // Get nearby hostile mobs to avoid
        double threatDetectionRadius = 16.0;
        java.util.List<SleepEntity> nearbyHostiles = context.getWorld().getNearbyHostiles(
                currentPos.getX(), currentPos.getY(), currentPos.getZ(), threatDetectionRadius
        );

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    SleepBlockPos pos = currentPos.offset(x, y, z);

                    // Check if site is within max distance (using Euclidean distance)
                    double distanceFromCurrent = Math.sqrt(pos.distSqr(currentPos));
                    if (distanceFromCurrent > searchRadius) {
                        continue;
                    }

                    // Check if site is too close to hostile mobs
                    boolean tooCloseToHostile = false;
                    for (SleepEntity entity : nearbyHostiles) {
                        if (!entity.isAlive()) {
                            continue;
                        }

                        String entityString = entity.getEntityType().toLowerCase();
                        boolean isHostile = entityString.contains("zombie")
                                || entityString.contains("skeleton")
                                || entityString.contains("creeper")
                                || entityString.contains("spider")
                                || entityString.contains("phantom");

                        if (isHostile) {
                            double distanceToHostile = Math.sqrt(entity.distanceToSqr(
                                    pos.getX(), pos.getY(), pos.getZ()
                            ));

                            // Avoid sleeping within 8 blocks of hostile mobs
                            if (distanceToHostile < 8.0) {
                                tooCloseToHostile = true;
                                break;
                            }
                        }
                    }

                    if (tooCloseToHostile) {
                        continue; // Skip this position
                    }

                    double score = scoreSleepSite(pos, context);
                    if (score > bestScore) {
                        bestScore = score;
                        bestSite = pos;
                    }
                }
            }
        }

        return bestSite != null ? bestSite : currentPos;
    }

    /**
     * Scores a potential sleep site based on configuration preferences.
     */
    public double scoreSleepSite(SleepBlockPos pos, SleepContext context) {
        double score = 0.0;

        // Check if covered (has blocks above)
        if (config.isPreferCoveredSites()) {
            SleepBlockInfo above = context.getBlockInfo(pos.getX(), pos.getY() + 1, pos.getZ());
            if (!above.isAir() && !above.isLeaves()) {
                score += 10.0; // Covered sites are preferred
            }
        }

        // Check if elevated (relative to a baseline, e.g., Y=64)
        if (config.isPreferElevatedSites()) {
            // Higher positions get more points
            double elevationBonus = (pos.getY() - 64) * 0.5;
            if (elevationBonus > 0) {
                score += elevationBonus;
            }
        }

        // Check for solid ground below
        SleepBlockInfo below = context.getBlockInfo(pos.getX(), pos.getY() - 1, pos.getZ());
        if (!below.isAir() && below.isSolid()) {
            score += 3.0;
        }

        return score;
    }

    /**
     * Checks if a site is safe from threats (covered, concealed).
     */
    public boolean isSiteSafe(SleepContext context, SleepBlockPos pos) {
        SleepBlockInfo above = context.getBlockInfo(pos.getX(), pos.getY() + 1, pos.getZ());

        // Site is safe if covered or concealed
        if (!above.isAir() && !above.isLeaves()) {
            return true;
        }

        return false;
    }

    /**
     * Simulates a tick passing.
     */
    public void tick(SleepContext context) {
        ticksSinceSiteCheck++;
    }

    /**
     * Sets the previous sleep site.
     */
    public void setPreviousSite(SleepBlockPos site) {
        this.previousSite = site;
    }

    /**
     * Gets the previous sleep site.
     */
    public SleepBlockPos getPreviousSite() {
        return previousSite;
    }

    /**
     * Gets ticks since last site check.
     */
    public int getTicksSinceSiteCheck() {
        return ticksSinceSiteCheck;
    }

    /**
     * Resets site tracking.
     */
    public void resetSiteTracking() {
        previousSite = null;
        ticksSinceSiteCheck = 0;
    }
}
