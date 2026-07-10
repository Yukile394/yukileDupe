package com.yukile.copyitem;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModPackets {
    public static final CustomPayload.Id<CopyItemPayload> COPY_PACKET_ID = 
        new CustomPayload.Id<>(Identifier.of("yukiledupe", "copy_packet"));

    // Paket verisini taşıyan kayıt yapısı
    public record CopyItemPayload(int miktar) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, CopyItemPayload> CODEC = CustomPayload.codec(
            (payload, buf) -> buf.writeInt(payload.miktar()),
            buf -> new CopyItemPayload(buf.readInt())
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return COPY_PACKET_ID;
        }
    }

    public static void registerPackets() {
        // Alıcı-Verici veri tipini kaydet
        PayloadTypeRegistry.playC2S().register(COPY_PACKET_ID, CopyItemPayload.CODEC);

        // SUNUCU TARAFI: Paketi aldığında eşyayı GERÇEK olarak üretir
        ServerPlayNetworking.registerGlobalReceiver(COPY_PACKET_ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                // Güvenlik Kontrolü: Oyuncu OP değilse işlem yapma
                if (!player.hasPermissionLevel(2)) return;

                int miktar = Math.min(payload.miktar(), 100); // En fazla 100 sınırı
                if (miktar <= 0) return;

                ItemStack held = player.getMainHandStack();
                if (held.isEmpty()) return;

                int maxStack = held.getMaxCount();
                int kalan = miktar;

                while (kalan > 0) {
                    int stackBoyutu = Math.min(kalan, maxStack);
                    ItemStack kopya = held.copy();
                    kopya.setCount(stackBoyutu);

                    if (!player.getInventory().insertStack(kopya)) {
                        player.dropItem(kopya, false, false);
                    }
                    kalan -= stackBoyutu;
                }

                // Hayalet İtem Engelleyici Altın Vuruş
                player.currentScreenHandler.sendContentUpdates();
                player.getInventory().markDirty();
            });
        });
    }
}
