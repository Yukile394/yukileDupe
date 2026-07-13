package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
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
    private int dupeModu = 0;
    private int syncId = -1;
    private boolean containerAcik = false;

    // HUD buton boyutları
    private static final int BTN_X = 10;
    private static final int BTN_Y = 10;
    private static final int BTN_GENISLIK = 95;
    private static final int BTN_YUKSEKLIK = 20;
    private static final int BTN_BOSLUK = 2;

    // Action ID sayacı
    private short actionId = 0;

    @Override
    public void onInitialize() {
        dupeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.start", GLFW.GLFW_KEY_O, "category.yukiledupe"));

        modKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.mode", GLFW.GLFW_KEY_P, "category.yukiledupe"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) return;

            if (modKey.wasPressed()) {
                dupeModu = (dupeModu + 1) % 3;
                String[] modlar = {"Book Dupe", "Container Dupe", "Drop Timing Dupe"};
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§eMod: §6" + modlar[dupeModu]), true);
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

        // HUD render - envanter açıkken butonları çiz
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.currentScreen instanceof InventoryScreen) {
                butonlariCiz(drawContext);
            }
        });
    }

    // ==================== HUD BUTONLARI ====================
    private void butonlariCiz(DrawContext ctx) {
        String[] modlar = {"Book", "Container", "Drop"};

        int y = BTN_Y;
        butonCiz(ctx, "▶ Baslat", BTN_X, y, !aktif ? 0xFF55FF55 : 0xFF666666);
        y += BTN_YUKSEKLIK + BTN_BOSLUK;
        butonCiz(ctx, "■ Durdur", BTN_X, y, aktif ? 0xFFFF5555 : 0xFF666666);
        y += BTN_YUKSEKLIK + BTN_BOSLUK;
        butonCiz(ctx, "Mod: " + modlar[dupeModu], BTN_X, y, 0xFF5555FF);
        y += BTN_YUKSEKLIK + BTN_BOSLUK;
        butonCiz(ctx, "⚡ Hizli x10", BTN_X, y, 0xFFFFFF55);
    }

    private void butonCiz(DrawContext ctx, String yazi, int x, int y, int renk) {
        MinecraftClient mc = MinecraftClient.getInstance();
        // Buton arkaplanı
        ctx.fill(x, y, x + BTN_GENISLIK, y + BTN_YUKSEKLIK, 0xCC000000);
        // Kenarlık
        ctx.fill(x, y, x + BTN_GENISLIK, y + 1, renk);
        ctx.fill(x, y + BTN_YUKSEKLIK - 1, x + BTN_GENISLIK, y + BTN_YUKSEKLIK, renk);
        ctx.fill(x, y, x + 1, y + BTN_YUKSEKLIK, renk);
        ctx.fill(x + BTN_GENISLIK - 1, y, x + BTN_GENISLIK, y + BTN_YUKSEKLIK, renk);
        // Yazı
        int yaziX = x + (BTN_GENISLIK - mc.textRenderer.getWidth(yazi)) / 2;
        int yaziY = y + (BTN_YUKSEKLIK - 8) / 2;
        ctx.drawTextWithShadow(mc.textRenderer, yazi, yaziX, yaziY, renk);
    }

    private short yeniActionId() {
        actionId++;
        return actionId;
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
            int bosSlot = bosSlotBul(p);

            if (bosSlot != -1) {
                for (int i = 0; i < 3; i++) {
                    c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                            p.currentScreenHandler.syncId,
                            p.currentScreenHandler.getRevision(),
                            hedefSlot,
                            0,
                            SlotActionType.PICKUP,
                            p.getInventory().getStack(hedefSlot).copy(),
                            yeniActionId()
                    ));
                }
                basarili++;
                c.execute(() -> p.sendMessage(Text.literal("§a[✓] Book Dupe Basarili! (#" + basarili + ")"), true));
            } else {
                basarisiz++;
                c.execute(() -> p.sendMessage(Text.literal("§c[✗] Envanterde bos slot yok! (#" + basarisiz + ")"), true));
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
                    hedef != null ? hedef.copy() : ItemStack.EMPTY,
                    yeniActionId()
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
                syncId = p.currentScreenHandler.syncId;
                faz = 4;
                beklemeTick = 0;
            } else {
                durdur(c);
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
                    yeniActionId()
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
            if (el.isEmpty() && !envanterdeAra(c)) {
                p.sendMessage(Text.literal("§cDupelenecek item yok!"), false);
                durdur(c);
                return;
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
