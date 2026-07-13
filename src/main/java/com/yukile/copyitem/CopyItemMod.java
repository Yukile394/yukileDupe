package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class CopyItemMod implements ModInitializer {

    private static KeyBinding dupeKey;
    private static KeyBinding modKey;
    private boolean aktif = false;
    private int basarili = 0;
    private int basarisiz = 0;
    private int slot = -1;
    private ItemStack hedef = null;
    private final Random rastgele = new Random();
    private int beklemeTick = 0;
    private int faz = 0;
    private int dupeModu = 0; // 0: Book, 1: Container, 2: Drop
    private int syncId = -1;
    private boolean containerAcik = false;

    // GUI Butonları
    private ButtonWidget baslatBtn;
    private ButtonWidget durdurBtn;
    private ButtonWidget modBtn;
    private ButtonWidget hizliDupeBtn;
    private boolean envanterAcik = false;
    private int modSayaci = 0;

    @Override
    public void onInitialize() {
        dupeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.start", GLFW.GLFW_KEY_O, "category.yukiledupe"));

        modKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.mode", GLFW.GLFW_KEY_P, "category.yukiledupe"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) return;

            // Envanter açık mı kontrol et
            boolean simdiAcik = client.currentScreen instanceof InventoryScreen;
            if (simdiAcik && !envanterAcik) {
                envanterAcildi(client);
            } else if (!simdiAcik && envanterAcik) {
                envanterKapandi();
            }
            envanterAcik = simdiAcik;

            if (modKey.wasPressed()) {
                modSayaci = (modSayaci + 1) % 3;
                dupeModu = modSayaci;
                String[] modlar = {"Book Dupe", "Container Dupe", "Drop Timing Dupe"};
                client.player.sendMessage(Text.literal("§eMod: §6" + modlar[dupeModu]), true);
                if (modBtn != null) {
                    modBtn.setMessage(Text.literal("Mod: " + modlar[dupeModu]));
                }
            }

            if (dupeKey.wasPressed()) {
                if (!aktif) baslat(client);
                else durdur(client);
            }

            if (aktif) {
                switch (dupeModu) {
                    case 0 -> bookDupe(client);
                    case 1 -> containerDupe(client);
                    case 2 -> dropTimingDupe(client);
                }
            }
        });
    }

    // ==================== ENVANTER GUI ====================
    private void envanterAcildi(MinecraftClient client) {
        InventoryScreen ekran = (InventoryScreen) client.currentScreen;
        int x = ekran.x - 105; // Sol üst
        int y = ekran.y + 5;

        String[] modlar = {"Book", "Container", "Drop"};

        // Başlat Butonu
        baslatBtn = ButtonWidget.builder(
                Text.literal("▶ Başlat"),
                btn -> {
                    if (!aktif) baslat(client);
                })
                .dimensions(x, y, 95, 20)
                .build();

        // Durdur Butonu
        durdurBtn = ButtonWidget.builder(
                Text.literal("■ Durdur"),
                btn -> {
                    if (aktif) durdur(client);
                })
                .dimensions(x, y + 22, 95, 20)
                .build();

        // Mod Değiştir Butonu
        modBtn = ButtonWidget.builder(
                Text.literal("Mod: " + modlar[dupeModu]),
                btn -> {
                    dupeModu = (dupeModu + 1) % 3;
                    modSayaci = dupeModu;
                    btn.setMessage(Text.literal("Mod: " + modlar[dupeModu]));
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§eMod: §6" + modlar[dupeModu]), true);
                    }
                })
                .dimensions(x, y + 44, 95, 20)
                .build();

        // Hızlı Dupe Butonu (Tek tıkla 10 kez dene)
        hizliDupeBtn = ButtonWidget.builder(
                Text.literal("⚡ Hızlı x10"),
                btn -> {
                    if (client.player != null && !aktif) {
                        ItemStack el = client.player.getMainHandStack();
                        if (el.isEmpty()) {
                            client.player.sendMessage(Text.literal("§cEline item al!"), false);
                            return;
                        }
                        // 10 kez dupe dene
                        new Thread(() -> {
                            for (int i = 0; i < 10; i++) {
                                if (!aktif) baslat(client);
                                try { Thread.sleep(100); } catch (Exception e) {}
                            }
                        }).start();
                    }
                })
                .dimensions(x, y + 66, 95, 20)
                .build();

        // Butonları ekrana ekle
        ekran.addDrawableChild(baslatBtn);
        ekran.addDrawableChild(durdurBtn);
        ekran.addDrawableChild(modBtn);
        ekran.addDrawableChild(hizliDupeBtn);
    }

    private void envanterKapandi() {
        baslatBtn = null;
        durdurBtn = null;
        modBtn = null;
        hizliDupeBtn = null;
    }

    // ==================== MOD 0: BOOK DUPE ====================
    private void bookDupe(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        beklemeTick++;

        if (faz == 0 && beklemeTick >= 2) {
            ItemStack el = p.getMainHandStack();
            if (el.isEmpty() || (!el.getItem().toString().contains("book") && !el.getItem().toString().contains("writable"))) {
                p.sendMessage(Text.literal("§cEline writable book al!"), false);
                durdur(c);
                return;
            }

            BlockPos pos = p.getBlockPos();
            c.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                    net.minecraft.util.Hand.MAIN_HAND,
                    new net.minecraft.util.hit.BlockHitResult(
                            p.getPos().add(0, -1, 0),
                            Direction.UP,
                            pos.down(),
                            false
                    ),
                    0
            ));
            faz = 1;
            beklemeTick = 0;
        }
        else if (faz == 1 && beklemeTick >= 10) {
            c.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(
                    p.getInventory().selectedSlot,
                    java.util.List.of("§k" + rastgele.nextInt(999999), "", ""),
                    java.util.Optional.of("Dupe_" + rastgele.nextInt(999))
            ));
            faz = 2;
            beklemeTick = 0;
        }
        else if (faz == 2 && beklemeTick >= 5) {
            int hedefSlot = slot;
            int boşSlot = bosSlotBul(p);

            if (boşSlot != -1) {
                for (int i = 0; i < 3; i++) {
                    c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                            p.currentScreenHandler.syncId,
                            p.currentScreenHandler.getRevision(),
                            hedefSlot,
                            0,
                            SlotActionType.PICKUP,
                            p.getInventory().getStack(hedefSlot).copy(),
                            p.currentScreenHandler.getNextActionId(p.getInventory())
                    ));
                }

                basarili++;
                c.execute(() -> p.sendMessage(Text.literal("§a[✓] Book Dupe Basarili! (#" + basarili + ")"), true));
            } else {
                basarisiz++;
                c.execute(() -> p.sendMessage(Text.literal("§c[✗] Envanterde boş slot yok! (#" + basarisiz + ")"), true));
            }

            faz = 0;
            beklemeTick = 0;
        }
    }

    // ==================== MOD 1: CONTAINER DUPE ====================
    private void containerDupe(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        beklemeTick++;

        if (faz == 0 && beklemeTick >= 2) {
            BlockPos sandikPos = sandikBul(p);
            if (sandikPos == null) {
                p.sendMessage(Text.literal("§cYakinda sandik bulunamadi!"), false);
                durdur(c);
                return;
            }

            c.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                    net.minecraft.util.Hand.MAIN_HAND,
                    new net.minecraft.util.hit.BlockHitResult(
                            sandikPos.toCenterPos(),
                            Direction.UP,
                            sandikPos,
                            false
                    ),
                    0
            ));
            containerAcik = true;
            syncId = p.currentScreenHandler.syncId;
            faz = 1;
            beklemeTick = 0;
        }
        else if (faz == 1 && beklemeTick >= 8) {
            c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId,
                    p.currentScreenHandler.getRevision(),
                    slot,
                    0,
                    SlotActionType.QUICK_MOVE,
                    hedef.copy(),
                    p.currentScreenHandler.getNextActionId(p.getInventory())
            ));
            faz = 2;
            beklemeTick = 0;
        }
        else if (faz == 2 && beklemeTick >= 3) {
            c.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
            containerAcik = false;
            faz = 3;
            beklemeTick = 0;
        }
        else if (faz == 3 && beklemeTick >= 4) {
            BlockPos sandikPos = sandikBul(p);
            if (sandikPos != null) {
                c.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                        net.minecraft.util.Hand.MAIN_HAND,
                        new net.minecraft.util.hit.BlockHitResult(
                                sandikPos.toCenterPos(),
                                Direction.UP,
                                sandikPos,
                                false
                        ),
                        0
                ));
                containerAcik = true;
                faz = 4;
                beklemeTick = 0;
            }
        }
        else if (faz == 4 && beklemeTick >= 5) {
            c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId,
                    p.currentScreenHandler.getRevision(),
                    0,
                    0,
                    SlotActionType.QUICK_MOVE,
                    ItemStack.EMPTY,
                    p.currentScreenHandler.getNextActionId(p.getInventory())
            ));

            c.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
            containerAcik = false;

            basarili++;
            c.execute(() -> p.sendMessage(Text.literal("§a[✓] Container Dupe Basarili! (#" + basarili + ")"), true));

            faz = 0;
            beklemeTick = 0;
        }
    }

    // ==================== MOD 2: DROP TIMING DUPE ====================
    private void dropTimingDupe(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        beklemeTick++;

        if (faz == 0 && beklemeTick >= 2) {
            ItemStack el = p.getMainHandStack();
            if (el.isEmpty()) {
                if (!envanterdeAra(c)) {
                    p.sendMessage(Text.literal("§cDupelenecek item yok!"), false);
                    durdur(c);
                    return;
                }
            }

            for (int i = 0; i < 5; i++) {
                c.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.DROP_ITEM,
                        BlockPos.ORIGIN,
                        Direction.DOWN
                ));
            }
            faz = 1;
            beklemeTick = 0;
        }
        else if (faz == 1 && beklemeTick >= 6) {
            int bosSlot = bosSlotBul(p);
            if (bosSlot != -1) {
                p.getInventory().selectedSlot = bosSlot;
                c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bosSlot));
            }
            faz = 2;
            beklemeTick = 0;
        }
        else if (faz == 2 && beklemeTick >= 2) {
            p.getInventory().selectedSlot = slot;
            c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

            c.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    p.getX(), p.getY() + 0.05, p.getZ(), true
            ));
            c.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    p.getX(), p.getY() - 0.05, p.getZ(), true
            ));

            basarili++;
            c.execute(() -> p.sendMessage(Text.literal("§a[✓] Drop Dupe Basarili! (#" + basarili + ")"), true));

            faz = 0;
            beklemeTick = 0;
        }
    }

    // ==================== YARDIMCI METODLAR ====================
    private void baslat(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        ItemStack el = p.getMainHandStack();

        if (el.isEmpty()) {
            p.sendMessage(Text.literal("§cElinde item yok!"), false);
            return;
        }

        slot = p.getInventory().selectedSlot;
        hedef = el.copy();
        aktif = true;
        basarili = 0;
        basarisiz = 0;
        faz = 0;
        beklemeTick = 0;

        String[] modAdlari = {"Book Dupe", "Container Dupe", "Drop Timing Dupe"};
        p.sendMessage(Text.literal("§a§lDUPER BASLADI!"), false);
        p.sendMessage(Text.literal("§eMod: §6" + modAdlari[dupeModu]), false);
        p.sendMessage(Text.literal("§eItem: §f" + hedef.getCount() + "x " + hedef.getName().getString()), false);

        // Buton durumunu güncelle
        if (baslatBtn != null) baslatBtn.active = false;
        if (durdurBtn != null) durdurBtn.active = true;
    }

    private void durdur(MinecraftClient c) {
        aktif = false;
        hedef = null;
        faz = 0;
        beklemeTick = 0;

        if (containerAcik && c.getNetworkHandler() != null) {
            c.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
            containerAcik = false;
        }

        if (c.player != null) {
            c.player.sendMessage(Text.literal("§c§lDUPER DURDU!"), false);
            c.player.sendMessage(Text.literal("§aBasarili: §f" + basarili + " §cBasarisiz: §f" + basarisiz), false);
        }

        // Buton durumunu güncelle
        if (baslatBtn != null) baslatBtn.active = true;
        if (durdurBtn != null) durdurBtn.active = false;
    }

    private int bosSlotBul(ClientPlayerEntity p) {
        for (int i = 0; i < 36; i++) {
            if (p.getInventory().getStack(i).isEmpty() && i != slot) {
                return i;
            }
        }
        return -1;
    }

    private BlockPos sandikBul(ClientPlayerEntity p) {
        BlockPos playerPos = p.getBlockPos();
        for (int x = -3; x <= 3; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (p.getWorld().getBlockState(pos).getBlock().toString().contains("chest") ||
                        p.getWorld().getBlockState(pos).getBlock().toString().contains("barrel") ||
                        p.getWorld().getBlockState(pos).getBlock().toString().contains("shulker")) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean envanterdeAra(MinecraftClient c) {
        if (c.player == null || hedef == null) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack s = c.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.isOf(hedef.getItem())) {
                c.player.getInventory().selectedSlot = i;
                slot = i;
                if (c.getNetworkHandler() != null)
                    c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                return true;
            }
        }
        return false;
    }
                    }
