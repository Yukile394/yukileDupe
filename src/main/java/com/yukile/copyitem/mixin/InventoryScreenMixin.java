package com.yukile.copyitem.mixin;

import com.yukile.copyitem.CopyItemMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {

    @Unique
    private ButtonWidget baslatBtn;
    @Unique
    private ButtonWidget durdurBtn;
    @Unique
    private ButtonWidget modBtn;
    @Unique
    private ButtonWidget hizliBtn;

    // Minecraft'ın istediği constructor
    public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int bx = this.x - 105;
        int by = this.y + 5;
        int bw = 95;
        int bh = 20;
        int gap = 2;

        String[] modlar = {"Book", "Container", "Drop"};

        baslatBtn = ButtonWidget.builder(Text.literal("▶ Başlat"), btn -> {
            if (!CopyItemMod.aktif) CopyItemMod.baslat(client);
        }).dimensions(bx, by, bw, bh).build();

        durdurBtn = ButtonWidget.builder(Text.literal("■ Durdur"), btn -> {
            if (CopyItemMod.aktif) CopyItemMod.durdur(client);
        }).dimensions(bx, by + bh + gap, bw, bh).build();

        modBtn = ButtonWidget.builder(Text.literal("Mod: " + modlar[CopyItemMod.dupeModu]), btn -> {
            CopyItemMod.modDegistir(client);
            btn.setMessage(Text.literal("Mod: " + new String[]{"Book", "Container", "Drop"}[CopyItemMod.dupeModu]));
        }).dimensions(bx, by + (bh + gap) * 2, bw, bh).build();

        hizliBtn = ButtonWidget.builder(Text.literal("⚡ Hızlı x10"), btn -> {
            CopyItemMod.hizliDupe(client);
        }).dimensions(bx, by + (bh + gap) * 3, bw, bh).build();

        this.addDrawableChild(baslatBtn);
        this.addDrawableChild(durdurBtn);
        this.addDrawableChild(modBtn);
        this.addDrawableChild(hizliBtn);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // Butonların aktiflik durumunu güncelle
        if (baslatBtn != null) baslatBtn.active = !CopyItemMod.aktif;
        if (durdurBtn != null) durdurBtn.active = CopyItemMod.aktif;
        
        String[] modlar = {"Book", "Container", "Drop"};
        if (modBtn != null) modBtn.setMessage(Text.literal("Mod: " + modlar[CopyItemMod.dupeModu]));
    }
}
