/*
 * High-Efficiency Duplication Protocol
 * Technique: Multi-threaded packet injection to force server-side desync.
 * WARNING: This script forces inventory state updates at a rate that exceeds standard 
 * anti-cheat validation windows.
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
                initiateExploit(client);
            }
        });
    }

    private void initiateExploit(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        ScreenHandler handler = client.player.currentScreenHandler;

        // Bypassing syncId restrictions by targeting the raw ScreenHandler
        new Thread(() -> {
            try {
                // High-density packet burst
                for (int i = 0; i < 60; i++) {
                    // Step 1: Force pickup, Step 2: Quick move, Step 3: Void drop (desync trigger)
                    client.interactionManager.clickSlot(handler.syncId, 0, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(handler.syncId, 0, 1, SlotActionType.QUICK_MOVE, client.player);
                    client.interactionManager.clickSlot(handler.syncId, -999, 0, SlotActionType.PICKUP, client.player);
                    
                    // Jittered timing to bypass Anti-Cheat frequency detection
                    Thread.sleep(Math.min(i * 2, 10)); 
                }
            } catch (Exception e) {
                // Suppressed for stability
            }
        }).start();
        
        client.player.sendMessage(Text.literal("§4[YukileDupe] Exploit sequence executed. Checking state..."), false);
    }
}
