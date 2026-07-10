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
        // Hile girişimini başlatacak tuş (Örn: 'O' harfi)
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
        
        // Oyuncunun şu an bir sandık, örs veya varil açmış olması gerekir (Senkronizasyon bozmak için)
        if (container != null && container.containerId != 0) {
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§e[YukileDupe] Paket senkronizasyon saldırısı başlatılıyor..."), 
                false
            );

            // Sunucuya milisaniyeler içinde ardı ardına envanter tıklama paketleri gönderiyoruz
            // Amaç: Sunucunun "Bu eşya nerede?" sorusuna kafasını karıştırmak
            for (int i = 0; i < 5; i++) {
                // Slot 0'daki (elindeki veya sandıktaki) eşyaya hızlıca çift tıklama ve hızlı taşıma (Shift-Click) paketleri yolluyor
                client.gameMode.handleInventoryMouseClick(
                    container.containerId, 
                    0, 
                    0, 
                    ClickType.QUICK_MOVE, 
                    client.player
                );
                
                client.gameMode.handleInventoryMouseClick(
                    container.containerId, 
                    0, 
                    1, 
                    ClickType.THROW, // Eşyayı yere fırlatma paketi
                    client.player
                );
            }
        } else {
            client.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§c[YukileDupe] Hata: Bir sandık veya konteyner açık olmalı!"), 
                false
            );
        }
    }
}
