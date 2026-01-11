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

public class BetterEcologyGameTests implements FabricGameTest {

    private static final TagKey<Item> MEAT_TAG = TagKey.create(Registries.ITEM,
            ResourceLocation.parse("better-ecology:meat"));

    @GameTest(template = "better-ecology-gametest:empty_platform")
    public void meatTagIncludesFish(GameTestHelper helper) {
        // Check items using the ItemStack.is() method which properly uses data-driven tags
        ItemStack codStack = new ItemStack(Items.COD);
        ItemStack salmonStack = new ItemStack(Items.SALMON);

        boolean codInTag = codStack.is(MEAT_TAG);
        boolean salmonInTag = salmonStack.is(MEAT_TAG);

        if (!codInTag || !salmonInTag) {
            helper.fail("Expected meat tag to include fish (cod: " + codInTag + ", salmon: " + salmonInTag + ")");
            return;
        }
        helper.succeed();
    }
}
