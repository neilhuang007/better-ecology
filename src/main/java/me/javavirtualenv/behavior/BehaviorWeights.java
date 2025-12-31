package me.javavirtualenv.behavior;

/**
 * Configuration class holding weights for different behavior types.
 * <p>
 * Weights control how strongly each behavior influences entity movement.
 * Higher values make behaviors more dominant. A weight of 0 effectively
 * disables that behavior.
 * <p>
 * These weights can be loaded from config files or modified at runtime
 * to tune ecosystem behavior. All weights default to reasonable values
 * for typical animal behaviors.
 *
 * <h2>Flocking Behaviors</h2>
 * <ul>
 *   <li><b>separation</b>: Avoid crowding neighbors (default: 1.5)</li>
 *   <li><b>alignment</b>: Steer towards average heading (default: 1.0)</li>
 *   <li><b>cohesion</b>: Move toward group center (default: 1.0)</li>
 * </ul>
 *
 * <h2>Foraging Behaviors</h2>
 * <ul>
 *   <li><b>foodSeek</b>: Move toward food sources (default: 2.0)</li>
 *   <li><b>waterSeek</b>: Move toward water (default: 1.5)</li>
 *   <li><b>shelterSeek</b>: Move toward shelter (default: 0.8)</li>
 * </ul>
 *
 * <h2>Predation Behaviors</h2>
 * <ul>
 *   <li><b>pursuit</b>: Chase prey (default: 2.0)</li>
 *   <li><b>evasion</b>: Flee from predators (default: 3.0)</li>
 * </ul>
 *
 * <h2>Territorial Behaviors</h2>
 * <ul>
 *   <li><b>territorialDefense</b>: Defend territory (default: 1.2)</li>
 *   <li><b>wander</b>: Random exploration (default: 0.5)</li>
 * </ul>
 *
 * <h2>Environmental Behaviors</h2>
 * <ul>
 *   <li><b>obstacleAvoidance</b>: Avoid obstacles (default: 2.5)</li>
 *   <li><b>lightPreference</b>: Seek/prefer light levels (default: 0.3)</li>
 * </ul>
 *
 * <h2>Bee Behaviors</h2>
 * <ul>
 *   <li><b>pollination</b>: Move to flowers and pollinate (default: 1.5)</li>
 *   <li><b>hive_return</b>: Return to hive with nectar (default: 2.0)</li>
 *   <li><b>waggle_dance</b>: Communicate food sources (default: 0.5)</li>
 *   <li><b>hive_defense</b>: Defend hive from threats (default: 2.5)</li>
 * </ul>
 *
 * <h2>Allay Behaviors</h2>
 * <ul>
 *   <li><b>item_collecting</b>: Seek and collect specific items (default: 2.0)</li>
 *   <li><b>sound_following</b>: Follow note block sounds and jukebox music (default: 1.8)</li>
 * </ul>
 *
 * <h2>Strider Behaviors</h2>
 * <ul>
 *   <li><b>lava_walking</b>: Walk on lava surface with heat resistance (default: 2.0)</li>
 *   <li><b>temperature_seeking</b>: Seek warm areas, freeze when cold (default: 1.5)</li>
 *   <li><b>riding</b>: Handle player riding mechanics (default: 1.0)</li>
 * </ul>
 *
 * <h2>Sniffer Behaviors</h2>
 * <ul>
 *   <li><b>sniffing</b>: Enhanced smell detection for seeds (default: 1.5)</li>
 *   <li><b>digging</b>: Dig for ancient seeds (default: 2.0)</li>
 *   <li><b>sniffer_social</b>: Parent teaching and communication (default: 1.2)</li>
 * </ul>
 *
 * <h2>Armadillo Behaviors</h2>
 * <ul>
 *   <li><b>predator_avoidance</b>: Flee from predators or roll up when cornered (default: 2.5)</li>
 * </ul>
 */
public class BehaviorWeights {

    // Flocking behavior weights
    private double separation = 1.5;
    private double alignment = 1.0;
    private double cohesion = 1.0;

    // Foraging behavior weights
    private double foodSeek = 2.0;
    private double waterSeek = 1.5;
    private double shelterSeek = 0.8;

    // Predation behavior weights
    private double pursuit = 2.0;
    private double evasion = 3.0;

    // Territorial behavior weights
    private double territorialDefense = 1.2;
    private double wander = 0.5;

    // Environmental behavior weights
    private double obstacleAvoidance = 2.5;
    private double lightPreference = 0.3;

    // Bee behavior weights
    private double pollination = 1.5;
    private double hive_return = 2.0;
    private double waggle_dance = 0.5;
    private double hive_defense = 2.5;

    // Allay behavior weights
    private double item_collecting = 2.0;
    private double sound_following = 1.8;

    // Strider behavior weights
    private double lava_walking = 2.0;
    private double temperature_seeking = 1.5;
    private double riding = 1.0;

    // Sniffer behavior weights
    private double sniffing = 1.5;
    private double digging = 2.0;
    private double snifferSocial = 1.2;

    // Armadillo behavior weights
    private double predatorAvoidance = 2.5;

    /**
     * Creates a new BehaviorWeights instance with default values.
     */
    public BehaviorWeights() {
    }

    /**
     * Creates a copy of this BehaviorWeights instance.
     */
    public BehaviorWeights copy() {
        BehaviorWeights copy = new BehaviorWeights();
        copy.separation = this.separation;
        copy.alignment = this.alignment;
        copy.cohesion = this.cohesion;
        copy.foodSeek = this.foodSeek;
        copy.waterSeek = this.waterSeek;
        copy.shelterSeek = this.shelterSeek;
        copy.pursuit = this.pursuit;
        copy.evasion = this.evasion;
        copy.territorialDefense = this.territorialDefense;
        copy.wander = this.wander;
        copy.obstacleAvoidance = this.obstacleAvoidance;
        copy.lightPreference = this.lightPreference;
        copy.pollination = this.pollination;
        copy.hive_return = this.hive_return;
        copy.waggle_dance = this.waggle_dance;
        copy.hive_defense = this.hive_defense;
        copy.item_collecting = this.item_collecting;
        copy.sound_following = this.sound_following;
        copy.lava_walking = this.lava_walking;
        copy.temperature_seeking = this.temperature_seeking;
        copy.riding = this.riding;
        copy.sniffing = this.sniffing;
        copy.digging = this.digging;
        copy.snifferSocial = this.snifferSocial;
        copy.predatorAvoidance = this.predatorAvoidance;
        return copy;
    }

    // Getters and setters for all weights

    public double getSeparation() {
        return separation;
    }

    public void setSeparation(double separation) {
        this.separation = separation;
    }

    public double getAlignment() {
        return alignment;
    }

    public void setAlignment(double alignment) {
        this.alignment = alignment;
    }

    public double getCohesion() {
        return cohesion;
    }

    public void setCohesion(double cohesion) {
        this.cohesion = cohesion;
    }

    public double getFoodSeek() {
        return foodSeek;
    }

    public void setFoodSeek(double foodSeek) {
        this.foodSeek = foodSeek;
    }

    public double getWaterSeek() {
        return waterSeek;
    }

    public void setWaterSeek(double waterSeek) {
        this.waterSeek = waterSeek;
    }

    public double getShelterSeek() {
        return shelterSeek;
    }

    public void setShelterSeek(double shelterSeek) {
        this.shelterSeek = shelterSeek;
    }

    public double getPursuit() {
        return pursuit;
    }

    public void setPursuit(double pursuit) {
        this.pursuit = pursuit;
    }

    public double getEvasion() {
        return evasion;
    }

    public void setEvasion(double evasion) {
        this.evasion = evasion;
    }

    public double getTerritorialDefense() {
        return territorialDefense;
    }

    public void setTerritorialDefense(double territorialDefense) {
        this.territorialDefense = territorialDefense;
    }

    public double getWander() {
        return wander;
    }

    public void setWander(double wander) {
        this.wander = wander;
    }

    public double getObstacleAvoidance() {
        return obstacleAvoidance;
    }

    public void setObstacleAvoidance(double obstacleAvoidance) {
        this.obstacleAvoidance = obstacleAvoidance;
    }

    public double getLightPreference() {
        return lightPreference;
    }

    public void setLightPreference(double lightPreference) {
        this.lightPreference = lightPreference;
    }

    public double getPollination() {
        return pollination;
    }

    public void setPollination(double pollination) {
        this.pollination = pollination;
    }

    public double getHiveReturn() {
        return hive_return;
    }

    public void setHiveReturn(double hive_return) {
        this.hive_return = hive_return;
    }

    public double getWaggleDance() {
        return waggle_dance;
    }

    public void setWaggleDance(double waggle_dance) {
        this.waggle_dance = waggle_dance;
    }

    public double getHiveDefense() {
        return hive_defense;
    }

    public void setHiveDefense(double hive_defense) {
        this.hive_defense = hive_defense;
    }

    public double getItem_collecting() {
        return item_collecting;
    }

    public void setItem_collecting(double item_collecting) {
        this.item_collecting = item_collecting;
    }

    public double getSound_following() {
        return sound_following;
    }

    public void setSound_following(double sound_following) {
        this.sound_following = sound_following;
    }

    public double getLava_walking() {
        return lava_walking;
    }

    public void setLava_walking(double lava_walking) {
        this.lava_walking = lava_walking;
    }

    public double getTemperature_seeking() {
        return temperature_seeking;
    }

    public void setTemperature_seeking(double temperature_seeking) {
        this.temperature_seeking = temperature_seeking;
    }

    public double getRiding() {
        return riding;
    }

    public void setRiding(double riding) {
        this.riding = riding;
    }

    public double getSniffing() {
        return sniffing;
    }

    public void setSniffing(double sniffing) {
        this.sniffing = sniffing;
    }

    public double getDigging() {
        return digging;
    }

    public void setDigging(double digging) {
        this.digging = digging;
    }

    public double getSnifferSocial() {
        return snifferSocial;
    }

    public void setSnifferSocial(double snifferSocial) {
        this.snifferSocial = snifferSocial;
    }

    public double getPredatorAvoidance() {
        return predatorAvoidance;
    }

    public void setPredatorAvoidance(double predatorAvoidance) {
        this.predatorAvoidance = predatorAvoidance;
    }
}
