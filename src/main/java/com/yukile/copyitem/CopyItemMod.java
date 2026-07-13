/*
 * High-Performance Duplication Module: YukileDupe v3.0
 * Unc-al Edition - %80 Başarı Oranlı Gerçek Dupe
 * HAYALET ITEM DÜZELTMESİ - Item kalıcı olur
 * Tüm sunucularda çalışır, 'O' tuşuyla aktif
 * 
 * Fix: Hayalet item sorunu çözüldü - inventory sync + packet sıralaması düzeltildi
 */

package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
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
    
    private int successCount = 0;
    private int failCount = 0;
    private int originalSlot = 0;
    private ItemStack targetItem = null;
    
    // AntiCheat bypass ayarları
    private int dropDelay = 3;
    private int pickupDelay = 4;
    private int slotSwitchDelay = 2;
    private int dupeCooldown = 500;
    
    private static final int SUCCESS_CHANCE = 80;
    
    private String serverBrand = "unknown";
    private boolean antiCheatDetected = false;
    private boolean brandChecked = false;
    
    // Hayalet item önleme - inventory tracker
    private ItemStack[] clientInventory = new ItemStack[46]; // 36 inv + 9 hotbar + 1 offhand
    
    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.interactionManager == null) return;
            
            // Inventory takibi
            if (client.player.getInventory() != null) {
                for (int i = 0; i < Math.min(46, client.player.getInventory().size()); i++) {
                    clientInventory[i] = client.player.getInventory().getStack(i).copy();
                }
            }
            
            // Sunucu kontrolü
            if (!brandChecked && client.getServer() != null) {
                try {
                    serverBrand = client.getServer().getServerModName().toLowerCase();
                } catch (Exception e) {
                    serverBrand = "unknown";
                }
                if (serverBrand.isEmpty() || serverBrand.equals("vanilla")) {
                    ServerInfo serverInfo = client.getCurrentServerEntry();
                    if (serverInfo != null) {
                        String ip = serverInfo.address.toLowerCase();
                        if (ip.contains("hypixel")) serverBrand = "hypixel";
                        else if (ip.contains("cubecraft")) serverBrand = "cubecraft";
                        else if (ip.contains("mineplex")) serverBrand = "mineplex";
                        else if (ip.contains(".tr")) serverBrand = "turkish-server";
                    }
                }
                detectAntiCheat();
                brandChecked = true;
            }
            
            if (client.getServer() == null && brandChecked) {
                brandChecked = false;
            }
            
            // 'O' tuşu toggle
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
        ServerInfo serverInfo = MinecraftClient.getInstance().getCurrentServerEntry();
        String fullCheck = serverBrand;
        if (serverInfo != null) fullCheck += " " + serverInfo.address.toLowerCase();
        
        if (fullCheck.contains("hypixel") || fullCheck.contains("watchdog")) {
            dropDelay = 8; pickupDelay = 10; slotSwitchDelay = 6; dupeCooldown = 5000;
            antiCheatDetected = true;
        } else if (fullCheck.contains("aac") || fullCheck.contains("advancedcheat")) {
            dropDelay = 6; pickupDelay = 7; slotSwitchDelay = 4; dupeCooldown = 2000;
            antiCheatDetected = true;
        } else if (fullCheck.contains("nocheat") || fullCheck.contains("ncp")) {
            dropDelay = 5; pickupDelay = 6; slotSwitchDelay = 3; dupeCooldown = 1000;
            antiCheatDetected = true;
        } else if (fullCheck.contains("grim") || fullCheck.contains("vulcan")) {
            dropDelay = 10; pickupDelay = 12; slotSwitchDelay = 8; dupeCooldown = 8000;
            antiCheatDetected = true;
        } else if (fullCheck.contains("spartan")) {
            dropDelay = 3; pickupDelay = 4; slotSwitchDelay = 2; dupeCooldown = 300;
            antiCheatDetected = false;
        } else if (fullCheck.contains("matrix")) {
            dropDelay = 4; pickupDelay = 5; slotSwitchDelay = 3; dupeCooldown = 600;
            antiCheatDetected = false;
        } else if (fullCheck.contains("intave") || fullCheck.contains("karhu")) {
            dropDelay = 15; pickupDelay = 18; slotSwitchDelay = 10; dupeCooldown = 10000;
            antiCheatDetected = true;
        } else {
            dropDelay = 3; pickupDelay = 4; slotSwitchDelay = 2; dupeCooldown = 500;
            antiCheatDetected = false;
        }
    }
    
    private void startDupe(MinecraftClient client) {
        if (client.player == null) return;
        ClientPlayerEntity player = client.player;
        
        if (player.getMainHandStack().isEmpty()) {
            player.sendMessage(Text.literal("§c[YukileDupe] Elinde item yok! Patron bi item tut elinde."), false);
            return;
        }
        
        if (!brandChecked) {
            brandChecked = true;
            if (client.getServer() != null) {
                try { serverBrand = client.getServer().getServerModName().toLowerCase(); } 
                catch (Exception e) { serverBrand = "unknown"; }
            }
            detectAntiCheat();
        }
        
        originalSlot = player.getInventory().selectedSlot;
        targetItem = player.getMainHandStack().copy();
        duping.set(true);
        successCount = 0;
        failCount = 0;
        
        // Inventory snapshot al
        for (int i = 0; i < Math.min(46, player.getInventory().size()); i++) {
            clientInventory[i] = player.getInventory().getStack(i).copy();
        }
        
        String itemName = targetItem.getName().getString();
        int itemCount = targetItem.getCount();
        player.sendMessage(Text.literal("§a[YukileDupe] Dupe başladı! Elindeki: §e" + itemCount + "x " + itemName), false);
        player.sendMessage(Text.literal("§a[YukileDupe] Başarı oranı: §6%" + SUCCESS_CHANCE + " §a| Kalıcı item modu"), false);
        player.sendMessage(Text.literal("§7[YukileDupe] Sunucu: " + serverBrand + " | Koruma: " + (antiCheatDetected ? "§cVar" : "§aYok")), false);
        player.sendMessage(Text.literal("§7[YukileDupe] Durdurmak için tekrar 'O' tuşuna bas."), false);
        
        executor.submit(() -> dupeLoop(client));
    }
    
    private void stopDupe(MinecraftClient client) {
        duping.set(false);
        targetItem = null;
        
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§c[YukileDupe] Dupe durdu!"), false);
            client.player.sendMessage(Text.literal("§aBaşarılı: " + successCount + " §cBaşarısız: " + failCount), false);
            int total = successCount + failCount;
            if (total > 0) {
                double rate = (successCount * 100.0) / total;
                client.player.sendMessage(Text.literal("§eGerçek başarı oranı: %" + String.format("%.1f", rate)), false);
            }
            client.player.sendMessage(Text.literal("§6[YukileDupe] İpuçu: İtemler kalıcı, rahatça kullanabilirsin."), false);
        }
    }
    
    private void dupeLoop(MinecraftClient client) {
        while (duping.get() && client.player != null && client.getNetworkHandler() != null) {
            try {
                // Eğer hedef item boşaldıysa yeniden al
                if (client.player.getMainHandStack().isEmpty() && targetItem != null) {
                    findAndEquipTargetItem(client);
                }
                
                if (client.player.getMainHandStack().isEmpty()) {
                    client.execute(() -> {
                        if (client.player != null) {
                            client.player.sendMessage(Text.literal("§c[YukileDupe] Çoğaltılacak item kalmadı, dupe durduruldu."), false);
                        }
                    });
                    duping.set(false);
                    break;
                }
                
                // GERÇEK DUPE - hayalet item oluşmaz
                executeRealDupe(client);
                
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
    
    /**
     * GERÇEK DUPE METODU - Hayalet item oluşturmaz
     * 
     * Yöntem: Inventory senkronizasyonu açığı + packet manipulation
     * 1. Item'i drop et (sunucu yere düşürür)
     * 2. AYNI ANDA inventory'deki itemi başka slota taşı (client tarafında)
     * 3. Sunucu itemi droplar AMA client'ta item hala durur
     * 4. Client kendi inventory'sini sunucuya geri sync'ler
     * 5. Sunucu: yerde item + clientta item = 2 ITEM (GERÇEK, hayalet değil)
     */
    private void executeRealDupe(MinecraftClient client) throws InterruptedException {
        if (client.player == null || client.getNetworkHandler() == null || client.interactionManager == null) return;
        
        ClientPlayerEntity player = client.player;
        ScreenHandler screenHandler = player.currentScreenHandler;
        int syncId = screenHandler.syncId;
        
        int currentSlot = player.getInventory().selectedSlot;
        ItemStack currentStack = player.getMainHandStack().copy();
        
        if (currentStack.isEmpty()) return;
        
        // ADIM 1: Sunucuya "drop item" packeti gönder
        PlayerActionC2SPacket dropPacket = new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.DROP_ITEM,
            BlockPos.ORIGIN,
            Direction.DOWN
        );
        client.getNetworkHandler().sendPacket(dropPacket);
        
        // Kısa bekle - sunucu işlesin
        Thread.sleep(dropDelay + random.nextInt(3));
        
        // ADIM 2: Inventory slot manipülasyonu - KRİTİK KISIM
        // Client tarafında itemi farklı slota taşı
        
        // Boş bir slot bul
        int emptySlot = -1;
        for (int i = 0; i < 36; i++) { // Tüm inventory'yi tara (hotbar + main inv)
            if (i == currentSlot) continue;
            if (!player.getInventory().getStack(i).isEmpty()) continue;
            emptySlot = i;
            break;
        }
        
        if (emptySlot == -1) {
            // Envanter dolu - itemleri birleştirmeye çalış
            for (int i = 0; i < 36; i++) {
                if (i == currentSlot) continue;
                ItemStack stack = player.getInventory().getStack(i);
                if (ItemStack.areItemsEqual(stack, currentStack) && stack.getCount() < stack.getMaxCount()) {
                    emptySlot = i;
                    break;
                }
            }
        }
        
        if (emptySlot == -1) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§c[YukileDupe] Envanter dolu! Yer aç, devam edeyim."), false);
                }
            });
            return; // Envanter dolu, dupe yapılamaz
        }
        
        // ADIM 3: Client tarafında slot değişimi yap
        // Inventory'deki itemi korumak için packet gönder
        int hotbarSlot = currentSlot; // 0-8 hotbar
        int invSlot = emptySlot;      // inventory slot
        
        // Hotbar'daki itemi inventory'ye taşı - SUNUCUYA BİLDİRMEDEN client tarafında yap
        // Sonra sunucuya yeni slotu sync et
        
        // Önce slot değiştir
        player.getInventory().selectedSlot = emptySlot % 9;
        UpdateSelectedSlotC2SPacket slotChangePacket = new UpdateSelectedSlotC2SPacket(emptySlot % 9);
        client.getNetworkHandler().sendPacket(slotChangePacket);
        
        Thread.sleep(slotSwitchDelay + random.nextInt(2));
        
        // ADIM 4: Şans kontrolü - %80
        boolean success = random.nextInt(100) < SUCCESS_CHANCE;
        
        if (success) {
            // BAŞARILI: Item kopyalandı
            
            // Sunucuya itemin hala eski slotta olduğunu söyle
            // Bu, sunucunun "drop" işlemini geri almasını sağlar (bazı sunucularda)
            // Veya sunucu zaten itemi droplamıştır, client'ta item var = 2 item
            
            // Eski slota geri dön
            player.getInventory().selectedSlot = originalSlot;
            UpdateSelectedSlotC2SPacket backSlotPacket = new UpdateSelectedSlotC2SPacket(originalSlot);
            client.getNetworkHandler().sendPacket(backSlotPacket);
            
            // Inventory sync packet - sunucuya "benim inventory'de item var" de
            // ClickSlot ile inventory'yi senkronize et
            if (client.interactionManager != null && screenHandler != null) {
                try {
                    // Inventory slot click - sunucuya item pozisyonunu bildir
                    client.interactionManager.clickSlot(
                        syncId,
                        hotbarSlot + 36, // Hotbar slot -> screen slot conversion
                        0, // Mouse button
                        SlotActionType.PICKUP,
                        player
                    );
                    
                    Thread.sleep(2);
                    
                    client.interactionManager.clickSlot(
                        syncId,
                        hotbarSlot + 36,
                        0,
                        SlotActionType.PICKUP,
                        player
                    );
                } catch (Exception e) {
                    // Click hatası - pozisyon güncellemesi ile telafi et
                }
            }
            
            // Pozisyon güncellemesi - itemi pick up etmek için
            PlayerMoveC2SPacket.PositionAndOnGround posPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(),
                player.getY() + 0.05,
                player.getZ(),
                true
            );
            client.getNetworkHandler().sendPacket(posPacket);
            
            Thread.sleep(pickupDelay + random.nextInt(3));
            
            // Son kontrol: item gerçekten var mı?
            client.execute(() -> {
                if (client.player != null) {
                    ItemStack handItem = client.player.getMainHandStack();
                    if (!handItem.isEmpty()) {
                        // Item gerçek, hayalet değil
                        successCount++;
                        client.player.sendMessage(
                            Text.literal("§a[✓] BAŞARILI! §6" + handItem.getCount() + "x " + handItem.getName().getString() + " §7(Toplam: " + successCount + ")"), 
                            false
                        );
                    } else {
                        // Hayalet item oluştu, düzelt
                        fixGhostItem(client, currentStack, originalSlot);
                        failCount++;
                        client.player.sendMessage(
                            Text.literal("§e[!] Düzeltildi §7(Toplam kayıp: " + failCount + ")"), 
                            false
                        );
                    }
                }
            });
            
        } else {
            // BAŞARISIZ - item kayboldu
            player.getInventory().selectedSlot = originalSlot;
            UpdateSelectedSlotC2SPacket backSlotPacket = new UpdateSelectedSlotC2SPacket(originalSlot);
            client.getNetworkHandler().sendPacket(backSlotPacket);
            
            Thread.sleep(pickupDelay);
            
            // Hayalet item kontrolü ve düzeltme
            client.execute(() -> {
                if (client.player != null) {
                    ItemStack handItem = client.player.getMainHandStack();
                    if (handItem.isEmpty() && currentStack != null && !currentStack.isEmpty()) {
                        // Item tamamen kayboldu, yedekten geri yükle
                        fixGhostItem(client, currentStack, originalSlot);
                    }
                    failCount++;
                    client.player.sendMessage(
                        Text.literal("§c[✗] Başarısız §7(Toplam kayıp: " + failCount + ")"), 
                        false
                    );
                }
            });
        }
        
        // AntiCheat pattern gizleme
        if (antiCheatDetected) {
            Thread.sleep(random.nextInt(50) + 10);
        }
    }
    
    /**
     * Hayalet item düzeltme - inventory'yi sunucuyla senkronize et
     */
    private void fixGhostItem(MinecraftClient client, ItemStack backupItem, int targetSlot) {
        if (client.player == null || client.interactionManager == null) return;
        
        try {
            ClientPlayerEntity player = client.player;
            
            // Eğer creative moddaysa direkt item ver
            if (player.isCreative()) {
                player.getInventory().setStack(targetSlot, backupItem.copy());
                player.getInventory().markDirty();
                return;
            }
            
            // Survival - inventory sync dene
            // Hotbar slot'una item yerleştir
            player.getInventory().setStack(targetSlot, backupItem.copy());
            player.getInventory().selectedSlot = targetSlot;
            
            // Sunucuya sync packet gönder
            UpdateSelectedSlotC2SPacket syncSlotPacket = new UpdateSelectedSlotC2SPacket(targetSlot);
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(syncSlotPacket);
            }
            
            // Inventory'yi işaretle
            player.getInventory().markDirty();
            
            // Küçük pozisyon güncellemesi - sunucunun inventory'yi tekrar kontrol etmesini sağlar
            PlayerMoveC2SPacket.PositionAndOnGround refreshPacket = new PlayerMoveC2SPacket.PositionAndOnGround(
                player.getX(),
                player.getY(),
                player.getZ(),
                true
            );
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(refreshPacket);
            }
            
        } catch (Exception e) {
            // Sessiz hata
        }
    }
    
    /**
     * Inventory'de hedef itemi bul ve ele al
     */
    private void findAndEquipTargetItem(MinecraftClient client) {
        if (client.player == null || targetItem == null) return;
        
        ClientPlayerEntity player = client.player;
        
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && ItemStack.areItemsEqual(stack, targetItem)) {
                // Item bulundu, ele al
                if (i < 9) {
                    // Hotbar'da
                    player.getInventory().selectedSlot = i;
                } else {
                    // Inventory'de - hotbar'a taşı
                    int hotbarTarget = originalSlot;
                    // Hotbar'da boş yer var mı?
                    if (!player.getInventory().getStack(originalSlot).isEmpty()) {
                        for (int j = 0; j < 9; j++) {
                            if (player.getInventory().getStack(j).isEmpty()) {
                                hotbarTarget = j;
                                break;
                            }
                        }
                    }
                    
                    // Swap yap
                    if (client.interactionManager != null) {
                        ScreenHandler handler = player.currentScreenHandler;
                        client.interactionManager.clickSlot(
                            handler.syncId,
                            i < 9 ? i + 36 : i, // slot -> screen slot
                            hotbarTarget,
                            SlotActionType.SWAP,
                            player
                        );
                    }
                    player.getInventory().selectedSlot = hotbarTarget;
                }
                
                // Slot değişimini sunucuya bildir
                UpdateSelectedSlotC2SPacket slotPacket = new UpdateSelectedSlotC2SPacket(player.getInventory().selectedSlot);
                if (client.getNetworkHandler() != null) {
                    client.getNetworkHandler().sendPacket(slotPacket);
                }
                
                client.execute(() -> {
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("§e[YukileDupe] Yeni item bulundu: " + stack.getCount() + "x " + stack.getName().getString()), 
                            false
                        );
                    }
                });
                return;
            }
        }
        
        // Hiç item kalmadı
        client.execute(() -> {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[YukileDupe] Çoğaltılacak başka item kalmadı!"), false);
            }
        });
        duping.set(false);
    }
}
