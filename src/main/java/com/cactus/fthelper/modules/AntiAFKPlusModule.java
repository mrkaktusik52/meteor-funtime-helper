package com.cactus.fthelper.modules;

import com.cactus.fthelper.FuntimeHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;

import java.util.Random;

public class AntiAFKPlusModule extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<Integer> minDelayBetweenMove = sgGeneral.add(new IntSetting.Builder()
        .name("min-delay-between-moves")
        .description("Min delay between moves (seconds)")
        .defaultValue(20)
        .min(1)
        .sliderMax(60)
        .build()
    );

    private final Setting<Integer> maxDelayBetweenMove = sgGeneral.add(new IntSetting.Builder()
        .name("max-delay-between-moves")
        .description("Max delay between moves (seconds)")
        .defaultValue(40)
        .min(1)
        .sliderMax(90)
        .build()
    );

    private final Setting<Integer> minTimeOfMove = sgGeneral.add(new IntSetting.Builder()
        .name("min-time-of-move")
        .description("Min movement duration (ticks)")
        .defaultValue(10)
        .min(1)
        .sliderMax(60)
        .build()
    );

    private final Setting<Integer> maxTimeOfMove = sgGeneral.add(new IntSetting.Builder()
        .name("max-time-of-move")
        .description("Max movement duration (ticks)")
        .defaultValue(30)
        .min(1)
        .sliderMax(60)
        .build()
    );

    private KeyBinding[] movementKeys;
    private final Random random = new Random();

    private KeyBinding currentKey = null;
    private int ticksLeft = 0;
    private boolean isMovingPhase = false;

    public AntiAFKPlusModule() {
        super(FuntimeHelper.CATEGORY, "anti-afk+", "Better than standard AntiAFK");
    }

    @Override
    public void onActivate() {
        movementKeys = new KeyBinding[]{
            mc.options.forwardKey,
            mc.options.backKey,
            mc.options.leftKey,
            mc.options.rightKey
        };

        isMovingPhase = false;
        currentKey = null;
        ticksLeft = getNextDelayTicks();
    }

    @Override
    public void onDeactivate() {
        // Release the key if module is disabled mid-movement
        if (currentKey != null) {
            currentKey.setPressed(false);
            currentKey = null;
        }
        isMovingPhase = false;
        ticksLeft = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Minecraft resets key states every tick, so we must re-press the key each tick
        if (isMovingPhase && currentKey != null) {
            currentKey.setPressed(true);
        }

        if (ticksLeft > 0) {
            ticksLeft--;
            return;
        }

        if (isMovingPhase) {
            // Movement phase ended — release key and start idle phase
            currentKey.setPressed(false);
            currentKey = null;
            isMovingPhase = false;
            ticksLeft = getNextDelayTicks();
        } else {
            // Idle phase ended — pick a random direction and start moving
            currentKey = movementKeys[random.nextInt(movementKeys.length)];
            isMovingPhase = true;
            ticksLeft = getNextMoveTicks();
        }
    }

    private int getNextDelayTicks() {
        int min = minDelayBetweenMove.get();
        int max = maxDelayBetweenMove.get();
        if (min > max) min = max;
        return (min + random.nextInt((max - min) + 1)) * 20; // seconds to ticks
    }

    private int getNextMoveTicks() {
        int min = minTimeOfMove.get();
        int max = maxTimeOfMove.get();
        if (min > max) min = max;
        return min + random.nextInt((max - min) + 1);
    }
}
