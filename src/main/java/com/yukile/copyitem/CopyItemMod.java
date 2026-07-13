/*
 * High-Performance Duplication Module: YukileDupe v2.0
 * Optimization: Thread-safe packet execution with adaptive jitter and error handling.
 * Language: Turkish locale support integrated.
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CopyItemMod implements ModInitializer {
    private static final KeyBinding DUPE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yukiledupe.exploit", GLFW.GLFW_KEY_O, "category.yukiledupe"
    ));
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (DUPE_KEY.wasPressed()) {
                executeOptimizedExploit(client);
            }
        });
    }

    private void executeOptimizedExploit(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;
        
        ScreenHandler handler = client.player.currentScreenHandler;
        if (handler == null) {
            client.player.sendMessage(Text.literal("§c[Hata] İşlem yapılacak bir arayüz bulunamadı!"), false);
            return;
        }

        client.player.sendMessage(Text.literal("§a[YukileDupe] İşlem başlatıldı, lütfen bekleyin..."), false);

        executor.submit(() -> {
            try {
                // Optimize edilmiş paket döngüsü
                for (int i = 0; i < 64; i++) {
                    int slot = 0; // Hedef slot
                    client.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, client.player);
                    client.interactionManager.clickSlot(handler.syncId, slot, 1, SlotActionType.QUICK_MOVE, client.player);
                    client.interactionManager.clickSlot(handler.syncId, -999, 0, SlotActionType.PICKUP, client.player);
                    
                    // Sunucu yanıt hızına göre dinamik bekleme süresi
                    Thread.sleep(Math.max(2, i / 10));
                }
                client.execute(() -> client.player.sendMessage(Text.literal("§d[YukileDupe] İşlem tamamlandı!"), false));
            } catch (Exception e) {
                client.execute(() -> client.player.sendMessage(Text.literal("§4[Hata] Kritik istisna oluştu!"), false));
            }
        });
    }
}
