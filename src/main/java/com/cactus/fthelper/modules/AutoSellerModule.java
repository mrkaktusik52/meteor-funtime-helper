package com.cactus.fthelper.modules;

import com.cactus.fthelper.FuntimeHelper;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvents;

public class AutoSellerModule extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<String> itemName = sgGeneral.add(new StringSetting.Builder()
        .name("item-name")
        .description("Display name (or part of it) of the item to sell")
        .defaultValue("")
        .build()
    );

    private final Setting<Integer> price = sgGeneral.add(new IntSetting.Builder()
        .name("price")
        .description("Sell price")
        .defaultValue(25000)
        .min(1000)
        .sliderMax(1000000)
        .build()
    );

    private final Setting<Integer> delayBeforeSell = sgGeneral.add(new IntSetting.Builder()
        .name("delay-before-sell")
        .description("Ticks to wait after chat trigger before starting")
        .defaultValue(10)
        .min(5)
        .sliderMax(200)
        .build()
    );

    private final Setting<Integer> swapDelay = sgGeneral.add(new IntSetting.Builder()
        .name("swap-delay")
        .description("Ticks to wait after swapping item to hand before sending /ah sell")
        .defaultValue(5)
        .min(1)
        .sliderMax(20)
        .build()
    );

    private final Setting<Boolean> sellOnActivate = sgGeneral.add(new BoolSetting.Builder()
        .name("sell-on-activate")
        .description("Immediately try to sell when module is enabled")
        .defaultValue(true)
        .build()
    );

    // State machine
    enum State { IDLE, WAITING, SWAPPING, RESTORING }

    private State state = State.IDLE;
    private int tickCounter = 0;
    private int savedSlot = -1;
    private int foundSlot = -1;
    private boolean needRestore = false;

    public AutoSellerModule() {
        super(FuntimeHelper.CATEGORY, "auto-seller", "Automatically re-sells items on /ah after they are bought.");
    }

    @Override
    public void onActivate() {
        resetState();
        if (sellOnActivate.get()) startSellSequence();
    }

    @Override
    public void onDeactivate() {
        resetState();
    }

    private void resetState() {
        state = State.IDLE;
        tickCounter = 0;
        savedSlot = -1;
        foundSlot = -1;
        needRestore = false;
    }

    private void startSellSequence() {
        state = State.WAITING;
        tickCounter = 0;
    }

    // Searches entire inventory (hotbar 0-8 + main 9-35) by display name
    private int findItemInInventory() {
        if (mc.player == null) return -1;
        String target = itemName.get().toLowerCase();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getName().getString().toLowerCase().contains(target)) return i;
        }
        return -1;
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof GameMessageS2CPacket packet)) return;
        String message = packet.content().getString().toLowerCase();

        // Item was bought — start sell sequence again
        if (message.contains("у вас купили") && message.contains("на /ah")) {
            if (state != State.IDLE) return;
            info("Item bought! Re-listing...");
            startSellSequence();
        }

        // Server rejected price — retry immediately
        if (message.contains("слишком дорого") && message.contains("ещё раз")) {
            warning("Price rejected by server, retrying...");
            ChatUtils.sendPlayerMsg("/ah sell " + price.get());
        }
    }

    // State machine: WAITING -> SWAPPING -> RESTORING -> IDLE
    @SuppressWarnings("unused")
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || state == State.IDLE) return;
        tickCounter++;

        switch (state) {

            // Wait before starting the sell process
            case WAITING -> {
                if (tickCounter < delayBeforeSell.get()) return;

                foundSlot = findItemInInventory();
                if (foundSlot == -1) {
                    warning("Item \"%s\" not found in inventory!", itemName.get());
                    mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK.value(), 1.0f, 1.0f);
                    resetState();
                    return;
                }

                savedSlot = mc.player.getInventory().getSelectedSlot();
                needRestore = true;

                if (foundSlot < 9) {
                    InvUtils.swap(foundSlot, false);
                } else {
                    // Move from main inventory to current hotbar slot
                    InvUtils.move().from(foundSlot).toHotbar(savedSlot);
                }

                state = State.SWAPPING;
                tickCounter = 0;
            }

            // Wait for server to register the held item, then send /ah sell
            case SWAPPING -> {
                if (tickCounter < swapDelay.get()) return;

                ChatUtils.sendPlayerMsg("/ah sell " + price.get());
                info("Sent: /ah sell %d", price.get());

                state = State.RESTORING;
                tickCounter = 0;
            }

            // Restore previous hotbar slot
            case RESTORING -> {
                if (tickCounter < 2) return;

                if (needRestore && foundSlot < 9) {
                    InvUtils.swap(savedSlot, false);
                }

                mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                info("Sell complete.");
                resetState();
            }
        }
    }
}
