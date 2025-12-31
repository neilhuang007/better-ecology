package me.javavirtualenv.item;

import me.javavirtualenv.BetterEcology;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Custom items for the Better Ecology mod.
 * Includes truffles that pigs can find while rooting.
 */
public final class ModItems {

    private ModItems() {
        throw new AssertionError("ModItems should not be instantiated");
    }

    public static final Item TRUFFLE = new Item(new FabricItemSettings()
            .rarity(Rarity.RARE)
            .food(new net.minecraft.world.food.FoodProperties.Builder()
                    .nutrition(6)
                    .saturationMod(0.6f)
                    .build())) {
        @Override
        public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable("item.better-ecology.truffle.tooltip"));
            super.appendHoverText(stack, level, tooltip, flag);
        }
    };

    public static void register() {
        registerItem("truffle", TRUFFLE);
        addToItemGroups();
        BetterEcology.LOGGER.info("Registered mod items");
    }

    private static void registerItem(String name, Item item) {
        net.minecraft.core.Registry.register(
            net.minecraft.core.registries.BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(BetterEcology.MOD_ID, name),
            item
        );
    }

    private static void addToItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(net.minecraft.core.registries.BuiltInRegistries.ITEM_GROUP
                .get(ResourceLocation.withDefaultNamespace("food_and_drinks")))
            .register(content -> {
                content.accept(TRUFFLE);
            });
    }
}
