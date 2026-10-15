package com.enderdragonplus.item;

import com.enderdragonplus.EnderDragonPlus;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EdpItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnderDragonPlus.MODID);
    public static final DeferredItem<EndingCrystalItem> ENDING_CRYSTAL = ITEMS.registerItem("ending_crystal", EndingCrystalItem::new, properties -> properties.rarity(Rarity.EPIC).stacksTo(16).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    private EdpItems() {
    }

    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.insertAfter(Items.END_CRYSTAL.getDefaultInstance(), ENDING_CRYSTAL.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
