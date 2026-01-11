package me.javavirtualenv.behavior.breeding;

/**
 * Manages breeding season detection and triggers.
 * Handles month-based seasons, photoperiod cues, and year-round breeding.
 *
 * This redesigned version works with the BreedingEntity interface,
 * making it testable without mocking Minecraft classes.
 *
 * Research sources:
 * - Bronson, F.H. (2009). Climate change and seasonal reproduction
 * - Paul, M.J., et al. (2008). Reproductive regulation by photoperiod
 * - Loudon, A., & Brinklow, B. (1992). Reproduction in mammals
 */
public class BreedingSeason {

    private final BreedingConfig config;

    public BreedingSeason(BreedingConfig config) {
        this.config = config;
    }

    /**
     * Checks if it is currently the breeding season for an entity.
     * Considers year-round breeders, month ranges, photoperiod triggers,
     * and modulating factors (food availability, temperature, stress).
     * Research: Bronson (2009), Paul et al. (2008) on environmental modulators.
     *
     * @param entity The entity to check
     * @return True if currently in breeding season
     */
    public boolean isBreedingSeason(BreedingEntity entity) {
        if (config.isYearRoundBreeding()) {
            return checkModulatingFactors(entity);
        }

        int currentMonth = calculateMonth(entity);

        boolean isInMonthRange;
        if (config.getBreedingSeasonStart() <= config.getBreedingSeasonEnd()) {
            isInMonthRange = currentMonth >= config.getBreedingSeasonStart() &&
                           currentMonth <= config.getBreedingSeasonEnd();
        } else {
            isInMonthRange = currentMonth >= config.getBreedingSeasonStart() ||
                           currentMonth <= config.getBreedingSeasonEnd();
        }

        if (!isInMonthRange) {
            return false;
        }

        if (config.isPhotoperiodTrigger()) {
            if (!checkPhotoperiodTrigger(entity)) {
                return false;
            }
        }

        return checkModulatingFactors(entity);
    }

    /**
     * Checks modulating factors that affect breeding readiness.
     * Research shows food availability, temperature, stress, and social
     * interactions modulate breeding season activation.
     * Reference: Bronson (2009) on climate and seasonal reproduction.
     *
     * @param entity The entity to check
     * @return True if modulating factors favor breeding
     */
    private boolean checkModulatingFactors(BreedingEntity entity) {
        double healthRatio = entity.getHealth() / entity.getMaxHealth();

        double minHealthForBreeding = config.getMinHealthForBreeding() * 0.8;
        if (healthRatio < minHealthForBreeding) {
            return false;
        }

        if (entity.getTemperature() < 0.3) {
            return false;
        }

        if (entity.getTemperature() > 0.9) {
            return false;
        }

        return true;
    }

    /**
     * Checks if photoperiod (day length) triggers are satisfied.
     * Many species breed when days reach a certain length.
     *
     * @param entity The entity to check
     * @return True if photoperiod conditions are met
     */
    public boolean checkPhotoperiodTrigger(BreedingEntity entity) {
        if (!config.isPhotoperiodTrigger()) {
            return true;
        }

        long dayTime = entity.getDayTime();
        return dayTime >= config.getMinDayLength();
    }

    /**
     * Calculates the progress through the breeding season.
     *
     * @param entity The entity to check
     * @return Progress from 0.0 (start) to 1.0 (end)
     */
    public double getSeasonProgress(BreedingEntity entity) {
        if (config.isYearRoundBreeding()) {
            return 0.5;
        }

        long totalDays = entity.getGameTime() / 24000L;
        int yearDay = (int) (totalDays % 360); // Day of year (0-359)

        int seasonStart = config.getBreedingSeasonStart();
        int seasonEnd = config.getBreedingSeasonEnd();

        int seasonStartDay = seasonStart * 30; // Convert month to day of year
        int seasonEndDay = (seasonEnd + 1) * 30 - 1; // End of seasonEnd month (inclusive)

        int daysIntoSeason;
        int seasonLength;

        if (seasonStart <= seasonEnd) {
            // Season doesn't wrap around year end
            if (yearDay < seasonStartDay || yearDay > seasonEndDay) {
                return 0.0; // Not in season
            }
            daysIntoSeason = yearDay - seasonStartDay + 1;
            seasonLength = seasonEndDay - seasonStartDay + 1;
        } else {
            // Season wraps around year end
            int seasonEndOfYear = 360;
            int seasonBeginningOfNextYear = seasonEndDay + 1;

            seasonLength = (seasonEndOfYear - seasonStartDay) + seasonBeginningOfNextYear;

            if (yearDay >= seasonStartDay) {
                daysIntoSeason = yearDay - seasonStartDay + 1;
            } else if (yearDay <= seasonEndDay) {
                daysIntoSeason = (seasonEndOfYear - seasonStartDay) + (yearDay + 1);
            } else {
                return 0.0; // Not in season
            }
        }

        if (seasonLength <= 0) {
            return 0.5;
        }

        double progress = (double) daysIntoSeason / seasonLength;
        return Math.max(0.0, Math.min(1.0, progress));
    }

    /**
     * Gets the number of months until breeding season starts.
     *
     * @param entity The entity to check
     * @return Months until season (0 if currently in season)
     */
    public int getMonthsUntilSeason(BreedingEntity entity) {
        if (isBreedingSeason(entity)) {
            return 0;
        }

        int currentMonth = calculateMonth(entity);
        int seasonStart = config.getBreedingSeasonStart();

        if (seasonStart > currentMonth) {
            return seasonStart - currentMonth;
        } else {
            return (12 - currentMonth) + seasonStart;
        }
    }

    /**
     * Calculates the current month from game time.
     * Assumes a 360-day year with 30-day months.
     *
     * @param entity The entity to check
     * @return Month number (0-11, where 0=January)
     */
    private int calculateMonth(BreedingEntity entity) {
        long totalDays = entity.getGameTime() / 24000L;
        return (int) ((totalDays % 360) / 30);
    }

    /**
     * Calculates the current day of the month.
     *
     * @param entity The entity to check
     * @return Day of month (1-30)
     */
    private int calculateDayOfMonth(BreedingEntity entity) {
        long totalDays = entity.getGameTime() / 24000L;
        return (int) (totalDays % 30) + 1;
    }

    /**
     * Checks if the breeding season is ending soon.
     *
     * @param entity The entity to check
     * @param thresholdDays Days remaining to consider "soon"
     * @return True if season ends within threshold days
     */
    public boolean isSeasonEndingSoon(BreedingEntity entity, int thresholdDays) {
        if (config.isYearRoundBreeding() || !isBreedingSeason(entity)) {
            return false;
        }

        double progress = getSeasonProgress(entity);
        double daysIntoSeason = progress * getSeasonLengthInDays();
        double daysUntilEnd = getSeasonLengthInDays() - daysIntoSeason;

        return daysUntilEnd <= thresholdDays;
    }

    /**
     * Gets the total length of the breeding season in days.
     */
    private int getSeasonLengthInDays() {
        int seasonStart = config.getBreedingSeasonStart();
        int seasonEnd = config.getBreedingSeasonEnd();

        if (seasonStart <= seasonEnd) {
            return (seasonEnd - seasonStart + 1) * 30;
        } else {
            return ((12 - seasonStart) + seasonEnd + 1) * 30;
        }
    }
}
