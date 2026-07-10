package com.yukile.copyitem;

import net.fabricmc.api.ModInitializer;

public class CopyItemMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Paket iletişim kanalını açıyoruz
        ModPackets.registerPackets();
    }
}

