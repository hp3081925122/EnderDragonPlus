package com.enderdragonplus.item;

import com.enderdragonplus.EnderDragonPlus;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class EdpItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EnderDragonPlus.MODID);
    public static final RegistryObject<EndingCrystalItem> ENDING_CRYSTAL = ITEMS.register("ending_crystal", () -> new EndingCrystalItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(16)));

    private EdpItems() {
    }

    public static void addToTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.getEntries().putAfter(Items.END_CRYSTAL.getDefaultInstance(), ENDING_CRYSTAL.get().getDefaultInstance(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
