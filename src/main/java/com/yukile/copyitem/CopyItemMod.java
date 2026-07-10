package com.yukile.copyitem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * CopyItemMod - Çok oyunculu sunucularla %100 uyumlu, hayalet item engelli gerçek kopyalama modu.
 */
public class CopyItemMod implements ModInitializer {

    private static final int MAX_MIKTAR = 6400;

    @Override
    public void onInitialize() {
        // Bu kayıt işlemi sayesinde komut hem sunucu konsolunda hem de tüm oyuncuların chat panelinde (sekme tamamlama ile) görünür.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("copyitem")
                .requires(source -> source.hasPermissionLevel(2)) // Sunucuda OP (Yönetici) olanlar kullanabilir
                .executes(CopyItemMod::sendUsage)
                .then(argument("miktar", IntegerArgumentType.integer(1, MAX_MIKTAR))
                    .executes(CopyItemMod::copyItem))
            )
        );
    }

    private static int sendUsage(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Bu komut sadece oyuncular tarafından kullanılabilir."));
            return 0;
        }

        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Elinizde bir item bulunmuyor.").formatted(Formatting.RED), false);
        } else {
            String itemId = held.getItem().toString();
            source.sendFeedback(() -> Text.literal("Elinizdeki item: ").formatted(Formatting.GRAY)
                .append(Text.literal(itemId).formatted(Formatting.YELLOW)), false);
        }

        source.sendFeedback(() -> Text.literal("--- CopyItem Kullanımı ---").formatted(Formatting.GOLD), false);
        source.sendFeedback(() -> Text.literal("/copyitem ").formatted(Formatting.GRAY)
            .append(Text.literal("- elinizdeki itemin adını gösterir").formatted(Formatting.WHITE)), false);
        source.sendFeedback(() -> Text.literal("/copyitem <miktar> ").formatted(Formatting.GRAY)
            .append(Text.literal("- elinizdeki itemi gerçek olarak kopyalar").formatted(Formatting.WHITE)), false);
        return 1;
    }

    private static int copyItem(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Bu komut sadece oyuncular tarafından kullanılabilir."));
            return 0;
        }

        int miktar = IntegerArgumentType.getInteger(context, "miktar");
        ItemStack held = player.getMainHandStack();

        if (held.isEmpty()) {
            source.sendFeedback(() -> Text.literal("Elinizde kopyalanacak bir item bulunmuyor.").formatted(Formatting.RED), false);
            return 0;
        }

        String itemId = held.getItem().toString();
        int maxStack = held.getMaxCount();
        int kalan = miktar;

        while (kalan > 0) {
            int stackBoyutu = Math.min(kalan, maxStack);
            
            // 1. GERÇEK KOPYALAMA: Eşyanın canı, büyüleri, özel isimleri dahil birebir klonunu üretir.
            ItemStack kopya = held.copy();
            kopya.setCount(stackBoyutu);

            // 2. SUNUCU TARAFLI ENVENTER GİRİŞİ: Eşyayı sunucunun ana veri tabanına işler.
            // Eğer oyuncunun envanteri doluysa, hayalet olmasın diye yere GERÇEK bir yere düşen eşya (Item Entity) olarak fırlatır.
            boolean basarili = player.getInventory().insertStack(kopya);
            if (!basarili || !kopya.isEmpty()) {
                player.dropItem(kopya, false, false); 
            }
            
            kalan -= stackBoyutu;
        }

        // 3. MULTIPLAYER PAKET SENKRONİZASYONU (HAYALET İTEM ENGELİ):
        // Sunucu, bu satır sayesinde oyuncunun bilgisayarına anında gizli bir paket gönderir.
        // Oyuncunun ekranı sıfırlanır ve yeni eşyalar tıkır tıkır görünür, yere atılabilir, sandığa konabilir hale gelir.
        player.currentScreenHandler.sendContentUpdates();
        player.getInventory().markDirty(); 

        source.sendFeedback(() -> Text.literal(itemId).formatted(Formatting.GREEN)
            .append(Text.literal(" isimli item ").formatted(Formatting.GRAY))
            .append(Text.literal(miktar + "x").formatted(Formatting.GREEN))
            .append(Text.literal(" olarak kopyalandı. (Diğer oyuncular ve sunucu tarafından doğrulanabilir)").formatted(Formatting.GRAY)), false);
        
        return 1;
    }
}

