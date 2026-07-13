package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
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
    public static boolean aktif = false;
    public static int basarili = 0;
    public static int basarisiz = 0;
    public static int slot = -1;
    public static ItemStack hedef = null;
    public static int dupeModu = 0;
    public static int faz = 0;
    public static int beklemeTick = 0;
    public static int syncId = -1;
    public static boolean containerAcik = false;
    private static final Random rastgele = new Random();

    @Override
    public void onInitialize() {
        dupeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.start", GLFW.GLFW_KEY_O, "category.yukiledupe"));

        modKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.mode", GLFW.GLFW_KEY_P, "category.yukiledupe"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.getNetworkHandler() == null) return;

            if (modKey.wasPressed()) {
                modDegistir(client);
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

    public static void modDegistir(MinecraftClient client) {
        dupeModu = (dupeModu + 1) % 3;
        String[] modlar = {"Book Dupe", "Container Dupe", "Drop Timing Dupe"};
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§eMod: §6" + modlar[dupeModu]), true);
        }
    }

    public static void baslat(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        if (p == null) return;
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

    public static void durdur(MinecraftClient c) {
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

    public static void hizliDupe(MinecraftClient c) {
        if (c.player == null) return;
        ItemStack el = c.player.getMainHandStack();
        if (el.isEmpty()) {
            c.player.sendMessage(Text.literal("§cEline item al!"), false);
            return;
        }
        c.player.sendMessage(Text.literal("§e⚡ Hizli dupe basliyor (10 deneme)..."), false);
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (!aktif) baslat(c);
                try { Thread.sleep(150); } catch (Exception e) {}
            }
        }).start();
    }

    // ==================== BOOK DUPE ====================
    private static void bookDupe(MinecraftClient c) {
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
                    new net.minecraft.util.hit.BlockHitResult(p.getPos().add(0, -1, 0), Direction.UP, pos.down(), false), 0));
            faz = 1; beklemeTick = 0;
        }
        else if (faz == 1 && beklemeTick >= 10) {
            c.getNetworkHandler().sendPacket(new BookUpdateC2SPacket(
                    p.getInventory().selectedSlot,
                    java.util.List.of("§k" + rastgele.nextInt(999999), "", ""),
                    java.util.Optional.of("Dupe_" + rastgele.nextInt(999))));
            faz = 2; beklemeTick = 0;
        }
        else if (faz == 2 && beklemeTick >= 5) {
            int bosSlot = bosSlotBul(p);
            if (bosSlot != -1) {
                for (int i = 0; i < 3; i++) {
                    c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                            p.currentScreenHandler.syncId, p.currentScreenHandler.getRevision(),
                            slot, 0, SlotActionType.PICKUP,
                            p.getInventory().getStack(slot).copy(), new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
                }
                basarili++;
                c.execute(() -> p.sendMessage(Text.literal("§a[✓] Book Dupe Basarili! (#" + basarili + ")"), true));
            } else { basarisiz++; }
            faz = 0; beklemeTick = 0;
        }
    }

    // ==================== CONTAINER DUPE ====================
    private static void containerDupe(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        beklemeTick++;

        if (faz == 0 && beklemeTick >= 2) {
            BlockPos sp = sandikBul(p);
            if (sp == null) { p.sendMessage(Text.literal("§cYakinda sandik yok!"), false); durdur(c); return; }
            c.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                    net.minecraft.util.Hand.MAIN_HAND,
                    new net.minecraft.util.hit.BlockHitResult(sp.toCenterPos(), Direction.UP, sp, false), 0));
            containerAcik = true; syncId = p.currentScreenHandler.syncId; faz = 1; beklemeTick = 0;
        }
        else if (faz == 1 && beklemeTick >= 8) {
            c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId, p.currentScreenHandler.getRevision(), slot, 0, SlotActionType.QUICK_MOVE,
                    hedef != null ? hedef.copy() : ItemStack.EMPTY, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
            faz = 2; beklemeTick = 0;
        }
        else if (faz == 2 && beklemeTick >= 3) {
            c.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
            containerAcik = false; faz = 3; beklemeTick = 0;
        }
        else if (faz == 3 && beklemeTick >= 4) {
            BlockPos sp = sandikBul(p);
            if (sp != null) {
                c.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(
                        net.minecraft.util.Hand.MAIN_HAND,
                        new net.minecraft.util.hit.BlockHitResult(sp.toCenterPos(), Direction.UP, sp, false), 0));
                containerAcik = true; syncId = p.currentScreenHandler.syncId; faz = 4; beklemeTick = 0;
            } else { durdur(c); }
        }
        else if (faz == 4 && beklemeTick >= 5) {
            c.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                    syncId, p.currentScreenHandler.getRevision(), 0, 0, SlotActionType.QUICK_MOVE,
                    ItemStack.EMPTY, new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
            c.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(syncId));
            containerAcik = false; basarili++;
            c.execute(() -> p.sendMessage(Text.literal("§a[✓] Container Dupe Basarili! (#" + basarili + ")"), true));
            faz = 0; beklemeTick = 0;
        }
    }

    // ==================== DROP TIMING DUPE ====================
    private static void dropTimingDupe(MinecraftClient c) {
        ClientPlayerEntity p = c.player;
        beklemeTick++;

        if (faz == 0 && beklemeTick >= 2) {
            ItemStack el = p.getMainHandStack();
            if (el.isEmpty() && !envanterdeAra(c)) { p.sendMessage(Text.literal("§cDupelenecek item yok!"), false); durdur(c); return; }
            for (int i = 0; i < 5; i++) {
                c.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            }
            faz = 1; beklemeTick = 0;
        }
        else if (faz == 1 && beklemeTick >= 6) {
            int bs = bosSlotBul(p);
            if (bs != -1) { p.getInventory().selectedSlot = bs; c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(bs)); }
            faz = 2; beklemeTick = 0;
        }
        else if (faz == 2 && beklemeTick >= 2) {
            p.getInventory().selectedSlot = slot;
            c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            c.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(), p.getY() + 0.05, p.getZ(), true));
            c.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(p.getX(), p.getY() - 0.05, p.getZ(), true));
            basarili++;
            c.execute(() -> p.sendMessage(Text.literal("§a[✓] Drop Dupe Basarili! (#" + basarili + ")"), true));
            faz = 0; beklemeTick = 0;
        }
    }

    private static int bosSlotBul(ClientPlayerEntity p) {
        for (int i = 0; i < 36; i++) { if (p.getInventory().getStack(i).isEmpty() && i != slot) return i; }
        return -1;
    }

    private static BlockPos sandikBul(ClientPlayerEntity p) {
        BlockPos pp = p.getBlockPos();
        for (int x = -3; x <= 3; x++) for (int y = -1; y <= 2; y++) for (int z = -3; z <= 3; z++) {
            BlockPos pos = pp.add(x, y, z);
            String name = p.getWorld().getBlockState(pos).getBlock().toString();
            if (name.contains("chest") || name.contains("barrel") || name.contains("shulker")) return pos;
        }
        return null;
    }

    private static boolean envanterdeAra(MinecraftClient c) {
        if (c.player == null || hedef == null) return false;
        for (int i = 0; i < 36; i++) {
            ItemStack s = c.player.getInventory().getStack(i);
            if (!s.isEmpty() && s.isOf(hedef.getItem())) {
                c.player.getInventory().selectedSlot = i; slot = i;
                if (c.getNetworkHandler() != null) c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(i));
                return true;
            }
        }
        return false;
    }
        }
