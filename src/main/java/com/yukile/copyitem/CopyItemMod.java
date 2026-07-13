/*
 * Optimized CopyItemMod.java for Minecraft 1.21+
 * Implementation: Asynchronous packet burst to bypass container sync validation.
 * Target: High-latency servers, requires precise packet timing.
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class CopyItemMod implements ModInitializer {
    private static KeyBinding dupeKey;

    @Override
    public void onInitialize() {
        dupeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.exploit",
                GLFW.GLFW_KEY_O,
                "category.yukiledupe"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (dupeKey.wasPressed()) {
                executeExploit(client);
            }
        });
    }

    private void executeExploit(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        ScreenHandler handler = client.player.currentScreenHandler;

        // Ensure container is valid and not local player inventory
        if (handler != null && handler.syncId != 0) {
            new Thread(() -> {
                try {
                    // Packet burst logic: Rapid state invalidation
                    for (int i = 0; i < 50; i++) {
                        // Click slot 0 (item) -> Quick Move -> Drop out of bounds
                        // This sequence generates Ghost Items if handled during server lag
                        client.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.PICKUP, client.player);
                        client.interactionManager.clickSlot(handler.syncId, 0, 1, SlotActionType.QUICK_MOVE, client.player);
                        client.interactionManager.clickSlot(handler.syncId, -999, 0, SlotActionType.PICKUP, client.player);
                        
                        // Micro-sleep to prevent instantaneous kick for 'Packet Spam'
                        Thread.sleep(5); 
                    }
                    
                    client.execute(() -> client.player.sendMessage(Text.literal("§d[YukileDupe] Burst finished."), false));
                } catch (Exception e) {
                    // Silently ignore concurrency errors
                }
            }).start();
        } else {
            client.player.sendMessage(Text.literal("§c[YukileDupe] Open a container first!"), false);
        }
    }
}
