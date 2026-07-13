/*
 * Fixed CopyItemMod.java for Minecraft 1.21+
 * Corrected imports for mappings (Mojang/Yarn mappings).
 * Note: 'KeyMapping' is 'KeyMapping' in Mojang mappings, 'Minecraft' is 'MinecraftClient'.
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding; // Corrected import
import net.minecraft.client.MinecraftClient;   // Corrected import
import net.minecraft.screen.slot.SlotActionType; // Corrected import
import net.minecraft.screen.ScreenHandler;      // Corrected import
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
            while (dupeKey.wasPressed()) {
                performExploit(client);
            }
        });
    }

    private void performExploit(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        ScreenHandler container = client.player.currentScreenHandler;
        
        if (container != null && container.syncId != 0) {
            for (int i = 0; i < 20; i++) {
                client.interactionManager.clickSlot(container.syncId, 0, 0, SlotActionType.PICKUP, client.player);
                client.interactionManager.clickSlot(container.syncId, 0, 1, SlotActionType.QUICK_MOVE, client.player);
                client.interactionManager.clickSlot(container.syncId, -999, 0, SlotActionType.PICKUP, client.player);
                
                try { Thread.sleep(2); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            
            client.player.sendMessage(net.minecraft.text.Text.literal("§aExploit sequence transmitted."), false);
        }
    }
}
