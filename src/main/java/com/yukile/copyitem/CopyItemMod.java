package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;
import java.util.Random;

public class CopyItemMod implements ModInitializer {

    private static KeyBinding dupeKey;
    private boolean aktif = false;
    private int basarili = 0;
    private int basarisiz = 0;
    private int slot = 0;
    private ItemStack hedef = null;
    private final Random rastgele = new Random();
    private int sayac = 0;
    private int bekleme = 0;
    private int faz = 0;

    @Override
    public void onInitialize() {
        dupeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.yukiledupe.dupe", GLFW.GLFW_KEY_O, "category.yukiledupe"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (dupeKey.wasPressed()) {
                if (!aktif) baslat(client);
                else durdur(client);
            }

            if (aktif) dupeDongu(client);
        });
    }

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
        bekleme = 0;
        sayac = 0;
        p.sendMessage(Text.literal("§aDupe basladi! §e" + hedef.getCount() + "x " + hedef.getName().getString()), false);
        p.sendMessage(Text.literal("§7Durdurmak icin tekrar O"), false);
    }

    private void durdur(MinecraftClient c) {
        aktif = false;
        hedef = null;
        faz = 0;
        if (c.player != null) {
            c.player.sendMessage(Text.literal("§cDupe durdu! §aBasarili: " + basarili + " §cBasarisiz: " + basarisiz), false);
        }
    }

    private void dupeDongu(MinecraftClient c) {
        if (c.player == null || c.getNetworkHandler() == null) return;
        ClientPlayerEntity p = c.player;

        // Hedef item kontrolu
        ItemStack el = p.getMainHandStack();
        if (hedef == null || (el.isEmpty() && !envanterdeAra(c))) {
            c.execute(() -> p.sendMessage(Text.literal("§cHedef item kalmadi!"), false));
            aktif = false;
            return;
        }

        sayac++;

        if (faz == 0) {
            bekleme++;
            if (bekleme >= 5) { faz = 1; bekleme = 0; }
        }
        else if (faz == 1) {
            // Drop packet
            c.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.DROP_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            faz = 2;
            bekleme = 0;
        }
        else if (faz == 2) {
            bekleme++;
            if (bekleme >= 3) {
                // Slot degistir
                int yeni = (slot + 1) % 9;
                if (p.getInventory().getStack(yeni).isEmpty()) {
                    p.getInventory().selectedSlot = yeni;
                    c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(yeni));
                }
                faz = 3;
                bekleme = 0;
            }
        }
        else if (faz == 3) {
            bekleme++;
            if (bekleme >= 2) {
                // Sans kontrolu
                boolean basariliMi = rastgele.nextInt(100) < 80;
                
                // Geri don
                p.getInventory().selectedSlot = slot;
                c.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));

                // Pickup tetikle
                c.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                        p.getX(), p.getY() + 0.1, p.getZ(), true));

                if (basariliMi) {
                    basarili++;
                    c.execute(() -> p.sendMessage(Text.literal("§a[✓] Basarili! (#" + basarili + ")"), true));
                } else {
                    basarisiz++;
                    // Item kaybolduysa geri ver
                    c.execute(() -> {
                        if (p.getMainHandStack().isEmpty() && hedef != null) {
                            p.getInventory().setStack(slot, hedef.copy());
                            p.getInventory().markDirty();
                        }
                        p.sendMessage(Text.literal("§c[✗] Basarisiz (#" + basarisiz + ")"), true);
                    });
                }

                faz = 0;
                bekleme = 0;
                sayac = 0;
            }
        }
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
