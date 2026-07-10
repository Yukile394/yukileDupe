package com.yukile.copyitem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class CopyItemClient implements ClientModInitializer {
    private static KeyBinding dupeTusu;

    @Override
    public void onInitializeClient() {
        // 'O' tuşuna basınca çalışacak şekilde ayarlandı
        dupeTusu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "Dupe Menüsünü Aç", 
            InputUtil.Type.KEYSYM, 
            GLFW.GLFW_KEY_O, 
            "CopyItem Modu"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (dupeTusu.wasPressed()) {
                if (client.player != null && client.player.hasPermissionLevel(2)) {
                    client.setScreen(new DupeScreen());
                }
            }
        });
    }
}

