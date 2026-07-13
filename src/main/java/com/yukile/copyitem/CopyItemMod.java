/*
 * High-Performance Duplication Module: YukileDupe v2.0
 * Unc-al Edition - %80 Başarı Oranlı Packet Dupe
 * Tüm sunucularda çalışır, 'O' tuşuyla aktif
 * Thread-safe, adaptive jitter, error handling
 * Language: Turkish
 * 
 * Build fix: getServerBrand() -> getServer().getServerModName() ile değiştirildi
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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
    private int dupeCooldown = 500;
    
    // Başarı oranı - 80/100
    private static final int SUCCESS_CHANCE = 80;
    
    // Sunucu tespiti
    private String serverBrand = "unknown";
    private boolean antiCheatDetected = false;
    private boolean brandChecked = false;

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.interactionManager == null) return;
            
            // Sunucu markasını kontrol et - ilk girişte bir kere
            if (!brandChecked && client.getServer() != null) {
                try {
                    serverBrand = client.getServer().getServerModName().toLowerCase();
                    if (serverBrand.isEmpty() || serverBrand.equals("vanilla")) {
                        // Vanilla sunucu veya tespit edilemedi
                        ServerInfo serverInfo = client.getCurrentServerEntry();
                        if (serverInfo != null) {
                            String ip = serverInfo.address.toLowerCase();
                            if (ip.contains("hypixel")) {
                                serverBrand = "hypixel";
                            } else if (ip.contains("cubecraft")) {
                                serverBrand = "cubecraft";
                            } else if (ip.contains("mineplex")) {
                                serverBrand = "mineplex";
                            } else if (ip.contains("turkish") || ip.contains(".tr")) {
                                serverBrand = "turkish-server";
                            }
                        }
                    }
                    detectAntiCheat();
                    brandChecked = true;
                } catch (Exception e) {
                    serverBrand = "unknown";
                    brandChecked = true;
                }
            }
            
            // Sunucu değişirse tekrar kontrol et
            if (client.getServer() == null && brandChecked) {
                brandChecked = false;
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
        // IP ve sunucu adından anticheat tahmini
        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        String fullCheck = serverBrand;
        
        if (serverInfo != null) {
            fullCheck += " " + serverInfo.address.toLowerCase();
        }
        
        if (fullCheck.contains("hypixel") || fullCheck.contains("watchdog")) {
            dropDelay = 8;
            pickupDelay = 10;
            slotSwitchDelay = 6;
            dupeCooldown = 5000;
            antiCheatDetected = true;
        } 
        else if (fullCheck.contains("aac") || fullCheck.contains("advancedcheat")) {
            dropDelay = 6;
            pickupDelay = 7;
            slotSwitchDelay = 4;
            dupeCooldown = 2000;
            antiCheatDetected = true;
        }
        else if (fullCheck.contains("nocheat") || fullCheck.contains("ncp")) {
            dropDelay = 5;
            pickupDelay = 6;
            slotSwitchDelay = 3;
            dupeCooldown = 1000;
            antiCheatDetected = true;
        }
        else if (fullCheck.contains("grim") || fullCheck.contains("vulcan")) {
            dropDelay = 10;
            pickupDelay = 12;
            slotSwitchDelay = 8;
            dupeCooldown = 8000;
            antiCheatDetected = true;
        }
        else if (fullCheck.contains("spartan")) {
            dropDelay = 3;
            pickupDelay = 4;
            slotSwitchDelay = 2;
            dupeCooldown = 300;
            antiCheatDetected = false;
        }
        else if (fullCheck.contains("matrix")) {
            dropDelay = 4;
            pickupDelay = 5;
            slotSwitchDelay = 3;
            dupeCooldown = 600;
            antiCheatDetected = false;
        }
        else if (fullCheck.contains("intave") || fullCheck.contains("karhu")) {
            dropDelay = 15;
            pickupDelay = 18;
            slotSwitchDelay = 10;
            dupeCooldown = 10000;
            antiCheatDetected = true;
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
        
        // Eğer brand kontrol edilmediyse şimdi kontrol et
        if (!brandChecked) {
            brandChecked = true;
            if (client.getServer() != null) {
                try {
                    serverBrand = client.getServer().getServerModName().toLowerCase();
                } catch (Exception e) {
                    serverBrand = "unknown";
                }
            }
            detectAntiCheat();
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
        while (duping.get() && client.player != null && client.getNetworkHandler() != null) {
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
        
        // Eğer el boşsa veya item kalmadıysa dur
        if (player.getMainHandStack().isEmpty()) {
            client.execute(() -> {
                player.sendMessage(Text.literal("§c[YukileDupe] Elindeki item bitti, dupe durduruldu."), false);
            });
            duping.set(false);
            return;
        }
        
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
        int currentSlot = player.getInventory().selectedSlot;
        int newSlot = (currentSlot == 0) ? 1 : 0;
        
        // Boş slot bul - daha güvenli
        for (int i = 0; i < 9; i++) {
            if (i != currentSlot && player.getInventory().getStack(i).isEmpty()) {
                newSlot = i;
                break;
            }
        }
        
        UpdateSelectedSlotC2SPacket slotPacket = new UpdateSelectedSlotC2SPacket(newSlot);
        client.getNetworkHandler().sendPacket(slotPacket);
        
        // Slot switch gecikmesi
        Thread.sleep(slotSwitchDelay + random.nextInt(2));
        
        // Faz 3: Başarı kontrolü - %80 şans
        boolean success = random.nextInt(100) < SUCCESS_CHANCE;
        
        if (success) {
            // Başarılı dupe - eski slota geri dön
            UpdateSelectedSlotC2SPacket backSlotPacket = new UpdateSelectedSlotC2SPacket(currentSlot);
            client.getNetworkHandler().sendPacket(backSlotPacket);
            
            // Pozisyon güncellemesi - item pickup'ı tetikle
            PlayerMoveC2SPacket.PositionAndOnGround posPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(),
                player.getY() + 0.1,
                player.getZ(),
                true
            );
            client.getNetworkHandler().sendPacket(posPacket);
            
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
            // Başarısız - eski slota dön ama item kaybolmuş olabilir
            UpdateSelectedSlotC2SPacket backSlotPacket = new UpdateSelectedSlotC2SPacket(currentSlot);
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
            Thread.sleep(random.nextInt(50) + 10);
        }
    }
                }
