package com.yukile.copyitem;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class DupeScreen extends Screen {
    private TextFieldWidget miktarKutusu;

    public DupeScreen() {
        super(Text.literal("Dupe Miktar Menüsü"));
    }

    @Override
    protected void init() {
        // Yazı kutusunu oluştur (Ekranın ortasında)
        this.miktarKutusu = new TextFieldWidget(this.textRenderer, this.width / 2 - 50, this.height / 2 - 30, 100, 20, Text.literal("Miktar"));
        this.miktarKutusu.setMaxLength(3); // En fazla 3 basamak (100 için)
        this.miktarKutusu.setText("1");
        this.addSelectableChild(this.miktarKutusu);
        this.setInitialFocus(this.miktarKutusu);

        // "Tamam" Butonu oluştur
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Tamam"), button -> {
            try {
                int miktar = Integer.parseInt(this.miktarKutusu.getText());
                if (miktar > 100) miktar = 100; // Sınır 100
                if (miktar < 1) miktar = 1;

                // SUNUCUYA PAKET GÖNDER (Gerçek eşya basımı için)
                ClientPlayNetworking.send(new ModPackets.CopyItemPayload(miktar));
                this.close(); // Menüyü kapat
            } catch (NumberFormatException e) {
                this.miktarKutusu.setText("1");
            }
        }).dimensions(this.width / 2 - 50, this.height / 2, 100, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        this.miktarKutusu.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false; // Çok oyunculuda oyunu durdurmasın
    }
}

