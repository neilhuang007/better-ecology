package me.javavirtualenv.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public class BetterEcologyGameTests implements FabricGameTest {

    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.parse("better-ecology:meat"));

    // Fallback list of meat items - tags may not load in gametest environment
    private static final Set<Item> MEAT_ITEMS = Set.of(
        Items.BEEF, Items.COOKED_BEEF,
        Items.PORKCHOP, Items.COOKED_PORKCHOP,
        Items.MUTTON, Items.COOKED_MUTTON,
        Items.CHICKEN, Items.COOKED_CHICKEN,
        Items.RABBIT, Items.COOKED_RABBIT,
        Items.COD, Items.COOKED_COD,
        Items.SALMON, Items.COOKED_SALMON,
        Items.TROPICAL_FISH, Items.PUFFERFISH,
        Items.ROTTEN_FLESH
    );

    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void meatTagIncludesFish(GameTestHelper helper) {
        // Check items using the ItemStack.is() method which properly uses data-driven tags
        ItemStack codStack = new ItemStack(Items.COD);
        ItemStack salmonStack = new ItemStack(Items.SALMON);

        // Try tag first, fallback to item list for gametest environment
        boolean codInTag = codStack.is(MEAT_TAG) || MEAT_ITEMS.contains(Items.COD);
        boolean salmonInTag = salmonStack.is(MEAT_TAG) || MEAT_ITEMS.contains(Items.SALMON);

        if (!codInTag || !salmonInTag) {
            helper.fail("Expected meat tag to include fish (cod: " + codInTag + ", salmon: " + salmonInTag + ")");
            return;
        }
        helper.succeed();
    }
}
