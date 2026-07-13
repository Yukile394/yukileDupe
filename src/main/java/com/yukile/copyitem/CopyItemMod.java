/*
 * Updated logic for CopyItemMod
 * Implementation: Force state desync using rapid interaction cycles.
 * Logic: Toggles inventory slot state rapidly to trigger server-side ghost items.
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.lwjgl.glfw.GLFW;

public class CopyItemMod implements ModInitializer {
    private static KeyMapping dupeKey;

    @Override
    public void onInitialize() {
        dupeKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.yukiledupe.exploit",
                GLFW.GLFW_KEY_O,
                "category.yukiledupe"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (dupeKey.consumeClick()) {
                attemptPacketDupe(client);
            }
        });
    }

    private void attemptPacketDupe(Minecraft client) {
        if (client.player == null || client.gameMode == null) return;

        AbstractContainerMenu container = client.player.containerMenu;
        
        if (container != null && container.containerId != 0) {
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6[YukileDupe] Eksploit döngüsü tetiklendi..."), 
                false
            );

            // Logic enhancement: Send contradictory slot actions to force item duplication via state mismatch
            for (int i = 0; i < 10; i++) {
                // Perform rapid Pickup then QuickMove to create a server-side ghost reference
                client.gameMode.handleInventoryMouseClick(
                    container.containerId, 
                    0, 
                    0, 
                    ClickType.PICKUP, 
                    client.player
                );
                
                client.gameMode.handleInventoryMouseClick(
                    container.containerId, 
                    0, 
                    1, 
                    ClickType.QUICK_MOVE, 
                    client.player
                );

                // Force server to drop the item pointer before validation completes
                client.gameMode.handleInventoryMouseClick(
                    container.containerId, 
                    -999, 
                    0, 
                    ClickType.PICKUP, 
                    client.player
                );
            }
            
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a[YukileDupe] Döngü tamamlandı. Eşya durumu kontrol ediliyor."), 
                false
            );
        } else {
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c[YukileDupe] Hata: Konteyner bulunamadı."), 
                false
            );
        }
    }
}
