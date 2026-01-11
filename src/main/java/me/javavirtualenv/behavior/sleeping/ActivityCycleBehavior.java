package me.javavirtualenv.behavior.sleeping;

/**
 * Behavior for managing activity cycles (diurnal, nocturnal, crepuscular).
 * Determines when an animal should be sleeping or active based on time of day.
 * Refactored to be testable without Minecraft dependencies.
 */
public class ActivityCycleBehavior {

    private final SleepingConfig config;
    private boolean isSleeping = false;
    private int accumulatedSleepTime = 0;
    private int sleepDebt = 0;

    public ActivityCycleBehavior(SleepingConfig config) {
        this.config = config;
    }

    /**
     * Determines if the animal should be sleeping at the current time.
     */
    public boolean shouldSleep(SleepContext context) {
        long dayTime = context.getDayTime() % 24000;
        int sleepStart = config.getSleepStart();
        int sleepEnd = config.getSleepEnd();

        switch (config.getActivityCycle()) {
            case DIURNAL:
                // Sleep at night: from sleepStart (e.g., 13000) through wraparound to sleepEnd (e.g., 1000)
                return dayTime >= sleepStart || dayTime <= sleepEnd;
            case NOCTURNAL:
                // Sleep during day: from sleepStart (e.g., 6000) to sleepEnd (e.g., 18000)
                return dayTime >= sleepStart && dayTime <= sleepEnd;
            case CREPUSCULAR:
                // Sleep during midday: from sleepStart to sleepEnd (e.g., 4000-9000)
                return dayTime >= sleepStart && dayTime <= sleepEnd;
            default:
                return false;
        }
    }

    /**
     * Determines if the animal should be active at the current time.
     */
    public boolean isActive(SleepContext context) {
        return !shouldSleep(context);
    }

    /**
     * Gets the current activity cycle type.
     */
    public SleepingConfig.ActivityCycle getActivityCycle() {
        return config.getActivityCycle();
    }

    /**
     * Sets the sleeping state.
     */
    public void setSleeping(boolean sleeping) {
        this.isSleeping = sleeping;
    }

    /**
     * Returns true if currently sleeping.
     */
    public boolean isSleeping() {
        return isSleeping;
    }

    /**
     * Calculates sleep needed based on config and sleep debt.
     */
    public int calculateSleepNeeded(SleepingConfig config) {
        return config.getSleepDuration() + sleepDebt;
    }

    /**
     * Simulates a tick passing.
     */
    public void tick(SleepContext context) {
        if (isSleeping) {
            accumulatedSleepTime++;
            // Reduce sleep debt while sleeping
            if (sleepDebt > 0) {
                sleepDebt--;
            }
            // Check if sleep requirements are met
            if (accumulatedSleepTime >= calculateSleepNeeded(config)) {
                isSleeping = false;
                resetAccumulatedSleepTime();
            }
        } else if (shouldSleep(context)) {
            // Accumulate sleep debt when should sleep but isn't
            sleepDebt++;
        }
    }

    /**
     * Gets accumulated sleep time.
     */
    public int getAccumulatedSleepTime() {
        return accumulatedSleepTime;
    }

    /**
     * Resets accumulated sleep time.
     */
    public void resetAccumulatedSleepTime() {
        this.accumulatedSleepTime = 0;
    }

    /**
     * Gets current sleep debt.
     */
    public int getSleepDebt() {
        return sleepDebt;
    }

    /**
     * Reduces sleep debt by specified amount.
     */
    public void reduceSleepDebt(int amount) {
        this.sleepDebt = Math.max(0, sleepDebt - amount);
    }

    /**
     * Sets the sleep debt directly.
     */
    public void setSleepDebt(int debt) {
        this.sleepDebt = Math.max(0, debt);
    }
}
