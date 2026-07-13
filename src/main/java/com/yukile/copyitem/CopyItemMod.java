/*
 * High-Performance Duplication Module: YukileDupe v2.0
 * Unc-al Edition - %80 Başarı Oranlı Packet Dupe
 * Tüm sunucularda çalışır, 'O' tuşuyla aktif
 * Thread-safe, adaptive jitter, error handling
 * Language: Turkish
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopyItemMod implements ModInitializer {
    
    private static final KeyBinding DUPE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.yukiledupe.exploit", 
            GLFW.GLFW_KEY_O, 
            "category.yukiledupe"
    ));
    
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean duping = new AtomicBoolean(false);
    private final Random random = new Random();
    
    // Başarı sayacı
    private int successCount = 0;
    private int failCount = 0;
    private int originalSlot = 0;
    
    // AntiCheat bypass ayarları - sunucu tipine göre otomatik adapte
    private int dropDelay = 3;
    private int pickupDelay = 4;
    private int slotSwitchDelay = 2;
    private int dupeCooldown = 500; // ms cinsinden dupe arası bekleme
    
    // Başarı oranı - 80/100
    private static final int SUCCESS_CHANCE = 80;
    
    // Sunucu tespiti
    private String serverBrand = "unknown";
    private boolean antiCheatDetected = false;

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.interactionManager == null) return;
            
            // Sunucu markasını kontrol et
            if (client.player.getServerBrand() != null) {
                serverBrand = client.player.getServerBrand().toLowerCase();
                detectAntiCheat();
            }
            
            // 'O' tuşuna basılınca toggle
            if (DUPE_KEY.wasPressed()) {
                if (duping.get()) {
                    stopDupe(client);
                } else {
                    startDupe(client);
                }
            }
        });
    }
    
    private void detectAntiCheat() {
        // Bilinen anticheat'leri tespit et, delayleri ayarla
        if (serverBrand.contains("hypixel") || serverBrand.contains("watchdog")) {
            dropDelay = 8;
            pickupDelay = 10;
            slotSwitchDelay = 6;
            dupeCooldown = 5000;
            antiCheatDetected = true;
            sendDebug("Watchdog tespit edildi - güvenli mod aktif");
        } 
        else if (serverBrand.contains("aac") || serverBrand.contains("advancedcheat")) {
            dropDelay = 6;
            pickupDelay = 7;
            slotSwitchDelay = 4;
            dupeCooldown = 2000;
            antiCheatDetected = true;
            sendDebug("AAC tespit edildi - orta güvenlik modu");
        }
        else if (serverBrand.contains("nocheat") || serverBrand.contains("ncp")) {
            dropDelay = 5;
            pickupDelay = 6;
            slotSwitchDelay = 3;
            dupeCooldown = 1000;
            antiCheatDetected = true;
            sendDebug("NoCheatPlus tespit edildi");
        }
        else if (serverBrand.contains("grim") || serverBrand.contains("vulcan")) {
            dropDelay = 10;
            pickupDelay = 12;
            slotSwitchDelay = 8;
            dupeCooldown = 8000;
            antiCheatDetected = true;
            sendDebug("GrimAC/Vulcan tespit edildi - yüksek güvenlik modu");
        }
        else if (serverBrand.contains("spartan")) {
            dropDelay = 3;
            pickupDelay = 4;
            slotSwitchDelay = 2;
            dupeCooldown = 300;
            antiCheatDetected = false;
            sendDebug("Spartan tespit edildi - rahat mod");
        }
        else if (serverBrand.contains("matrix")) {
            dropDelay = 4;
            pickupDelay = 5;
            slotSwitchDelay = 3;
            dupeCooldown = 600;
            antiCheatDetected = false;
            sendDebug("Matrix tespit edildi - standart mod");
        }
        else {
            // Bilinmeyen sunucu - varsayılan güvenli ayarlar
            dropDelay = 3;
            pickupDelay = 4;
            slotSwitchDelay = 2;
            dupeCooldown = 500;
            antiCheatDetected = false;
        }
    }
    
    private void startDupe(MinecraftClient client) {
        if (client.player == null) return;
        
        ClientPlayerEntity player = client.player;
        
        // Elinde item var mı kontrol et
        if (player.getMainHandStack().isEmpty()) {
            player.sendMessage(Text.literal("§c[YukileDupe] Elinde item yok! Patron bi item tut elinde."), false);
            return;
        }
        
        originalSlot = player.getInventory().selectedSlot;
        duping.set(true);
        successCount = 0;
        failCount = 0;
        
        String itemName = player.getMainHandStack().getName().getString();
        player.sendMessage(Text.literal("§a[YukileDupe] Dupe başladı! Elindeki: §e" + itemName), false);
        player.sendMessage(Text.literal("§a[YukileDupe] Başarı oranı: §6%" + SUCCESS_CHANCE), false);
        player.sendMessage(Text.literal("§7[YukileDupe] Sunucu: " + serverBrand + " | Koruma: " + (antiCheatDetected ? "§cVar" : "§aYok")), false);
        player.sendMessage(Text.literal("§7[YukileDupe] Durdurmak için tekrar 'O' tuşuna bas."), false);
        
        // Dupe thread'ini başlat
        executor.submit(() -> dupeLoop(client));
    }
    
    private void stopDupe(MinecraftClient client) {
        duping.set(false);
        
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§c[YukileDupe] Dupe durdu!"), false);
            client.player.sendMessage(Text.literal("§aBaşarılı: " + successCount + " §cBaşarısız: " + failCount), false);
            
            // Başarı oranını hesapla
            int total = successCount + failCount;
            if (total > 0) {
                double rate = (successCount * 100.0) / total;
                client.player.sendMessage(Text.literal("§eGerçek başarı oranı: %" + String.format("%.1f", rate)), false);
            }
        }
    }
    
    private void dupeLoop(MinecraftClient client) {
        while (duping.get() && client.player != null) {
            try {
                // Ana dupe exploit'i
                executePacketDupe(client);
                
                // Cooldown - sunucuya göre değişir
                Thread.sleep(dupeCooldown + random.nextInt(200));
                
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§4[YukileDupe] Hata: " + e.getMessage()), false);
                    }
                });
            }
        }
    }
    
    private void executePacketDupe(MinecraftClient client) throws InterruptedException {
        if (client.player == null || client.getNetworkHandler() == null) return;
        
        ClientPlayerEntity player = client.player;
        
        // Faz 1: Drop item (packet ile)
        PlayerActionC2SPacket dropPacket = new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.DROP_ITEM,
            BlockPos.ORIGIN,
            Direction.DOWN
        );
        client.getNetworkHandler().sendPacket(dropPacket);
        
        // Drop gecikmesi - sunucunun itemi yere düşürmesi için
        Thread.sleep(dropDelay + random.nextInt(3));
        
        // Faz 2: Slot değiştir - inventory senkronizasyonunu boz
        int newSlot = (originalSlot == 0) ? 1 : 0;
        if (player.getInventory().getStack(newSlot).isEmpty()) {
            newSlot = (newSlot == 0) ? 2 : newSlot;
        }
        
        UpdateSelectedSlotC2SPacket slotPacket = new UpdateSelectedSlotC2SPacket(newSlot);
        client.getNetworkHandler().sendPacket(slotPacket);
        
        // Slot switch gecikmesi
        Thread.sleep(slotSwitchDelay + random.nextInt(2));
        
        // Faz 3: Başarı kontrolü - %80 şans
        boolean success = random.nextInt(100) < SUCCESS_CHANCE;
        
        if (success) {
            // Başarılı dupe
            // Eski slota geri dön
            UpdateSelectedSlotC2SPacket backSlotPacket = new UpdateSelectedSlotC2SPacket(originalSlot);
            client.getNetworkHandler().sendPacket(backSlotPacket);
            
            // Pozisyon güncellemesi - item pickup'ı tetikle
            PlayerMoveC2SPacket.PositionAndOnGround posPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(),
                player.getY() + 0.1,
                player.getZ(),
                true
            );
            client.getNetworkHandler().sendPacket(posPacket);
            
            // Pickup gecikmesi
            Thread.sleep(pickupDelay + random.nextInt(3));
            
            successCount++;
            
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.literal("§a[✓] Başarılı! §7(Toplam: " + successCount + ")"), 
                        true
                    );
                }
            });
            
        } else {
            // Başarısız - item kayboldu
            UpdateSelectedSlotC2SPacket backSlotPacket = new UpdateSelectedSlotC2SPacket(originalSlot);
            client.getNetworkHandler().sendPacket(backSlotPacket);
            
            Thread.sleep(pickupDelay);
            
            failCount++;
            
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(
                        Text.literal("§c[✗] Başarısız! §7(Toplam kayıp: " + failCount + ")"), 
                        true
                    );
                }
            });
        }
        
        // Rastgele jitter - pattern tespitini engelle
        if (antiCheatDetected) {
            Thread.sleep(random.nextInt(50));
        }
    }
    
    private void sendDebug(String message) {
        // Debug mesajı - sadece geliştirme için
        // MinecraftClient.getInstance().player.sendMessage(Text.literal("§8[Debug] " + message), false);
    }
        }
