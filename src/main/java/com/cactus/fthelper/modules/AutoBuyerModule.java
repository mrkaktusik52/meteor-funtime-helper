package com.cactus.fthelper.modules;

import com.cactus.fthelper.FuntimeHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;

public class AutoBuyerModule extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<String> itemName = sgGeneral.add(new StringSetting.Builder()
        .name("item-name")
        .description("Item name that you need to buy from /ah")
        .defaultValue("")
        .build()
    );

    private final Setting<Integer> maxPrice = sgGeneral.add(new IntSetting.Builder()
        .name("max-price")
        .description("Max price")
        .defaultValue(1000000)
        .min(70000)
        .sliderMax(3000000)
        .build()
    );

    private enum State { IDLE, SEARCHING, BUYING, CONFIRMING }
    private State state = State.SEARCHING;

    @Override
    public void onActivate() {
        state = State.SEARCHING; // Сбрасываем стейт при включении модуля
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        switch (state) {
            case IDLE -> {}
            case SEARCHING -> {
                if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) return;

                for (Slot slot : handler.slots) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty()) {
                        info("Slot %d: %s", slot.getIndex(), stack.getName().getString());
                    }
                }
            }
        }
    }

    public AutoBuyerModule() {
        super(FuntimeHelper.CATEGORY, "auto-buyer", "Automatically finds and buys the cheapest items from /ah");
    }

}
