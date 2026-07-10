package com.yukile.copyitem;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * CopyItem - Ogretici / bilgilendirici amacli bir Fabric modu.
 *
 * /copyitem            -> elde tutulan itemin adini ve kullanim bilgisini yazar
 * /copyitem <miktar>   -> elde tutulan itemi silmeden, belirtilen miktarda kopyalayip
 *                         oyuncunun envanterine ekler
 */
public class CopyItemMod implements ModInitializer {

    private static final int MAX_MIKTAR = 6400;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(literal("copyitem")
                .requires(source -> source.hasPermissionLevel(2))
                .executes(CopyItemMod::sendUsage)
                .then(argument("miktar", IntegerArgumentType.integer(1, MAX_MIKTAR))
                    .executes(CopyItemMod::copyItem))
            )
        );
    }

    /** /copyitem (parametresiz) -> elimizdeki itemin adini ve kullanim bilgisini gosterir */
    private static int sendUsage(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Bu komut sadece oyuncular tarafindan kullanilabilir."));
            return 0;
        }

        ItemStack held = player.getMainHandStack();

        if (held.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§cElinizde bir item bulunmuyor."), false);
        } else {
            String itemId = held.getItem().toString();
            source.sendFeedback(() -> Text.literal("§7Elinizdeki item: §e" + itemId), false);
        }

        source.sendFeedback(() -> Text.literal("§6--- CopyItem Kullanim ---"), false);
        source.sendFeedback(() -> Text.literal("§7/copyitem §f- elinizdeki itemin adini gosterir"), false);
        source.sendFeedback(() -> Text.literal("§7/copyitem <miktar> §f- elinizdeki itemi silmeden kopyalar"), false);
        source.sendFeedback(() -> Text.literal("§7Ornek: §f/copyitem 27"), false);
        return 1;
    }

    /** /copyitem <miktar> -> elimizdeki itemi silmeden kopyalar */
    private static int copyItem(com.mojang.brigadier.context.CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception e) {
            source.sendError(Text.literal("Bu komut sadece oyuncular tarafindan kullanilabilir."));
            return 0;
        }

        int miktar = IntegerArgumentType.getInteger(context, "miktar");
        ItemStack held = player.getMainHandStack();

        if (held.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§cElinizde kopyalanacak bir item bulunmuyor."), false);
            return 0;
        }

        // Orijinal item asla silinmez / azaltilmaz, sadece kopyalari uretilir.
        String itemId = held.getItem().toString();
        int maxStack = held.getMaxCount();
        int kalan = miktar;

        while (kalan > 0) {
            int stackBoyutu = Math.min(kalan, maxStack);
            ItemStack kopya = held.copy();
            kopya.setCount(stackBoyutu);

            if (!player.giveItemStack(kopya)) {
                player.dropItem(kopya, false);
            }
            kalan -= stackBoyutu;
        }

        source.sendFeedback(() -> Text.literal(
            "§a" + itemId + " §7isimli item §a" + miktar + "x §7olarak kopyalandi. §8(orijinal item silinmedi)"
        ), false);
        return 1;
    }
}
