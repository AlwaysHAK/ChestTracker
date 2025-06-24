package red.jackf.chesttracker.impl.compat.mods.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import red.jackf.chesttracker.api.memory.Memory;
import red.jackf.chesttracker.api.memory.MemoryBankAccess;
import red.jackf.chesttracker.impl.ChestTracker;
import red.jackf.chesttracker.impl.util.ItemStacks;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

import java.util.Optional;

public class JadeClientContentsPreview implements IBlockComponentProvider {
    public static JadeClientContentsPreview INSTANCE = new JadeClientContentsPreview();
    private JadeClientContentsPreview() {}

    public static final ResourceLocation ID = ChestTracker.id("memory_preview");

    private static void possiblyAddItems(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config, Memory memory) {
        if (config.get(JadeIds.UNIVERSAL_ITEM_STORAGE) &&
                (accessor.getServerData().contains("JadeItemStorage") // < 15.8.0
                        || accessor.getServerData().contains(JadeIds.UNIVERSAL_ITEM_STORAGE.toString())) // >= 15.8.0
        )
            return; // don't do it if jade is handling it
        if (config.get(JadeIds.MC_FURNACE)
                && accessor.getBlock() instanceof AbstractFurnaceBlock &&
                (accessor.getServerData().contains("furnace") // < 15.7.0
                        || accessor.getServerData().contains(JadeIds.MC_FURNACE.toString()))) // >=15.7.0
            return; // don't do furnaces if handled so progress still shows

        var stacks = ItemStacks.flattenStacks(memory.items(), true);

        int max = config.getInt(accessor.showDetails() ? JadeIds.UNIVERSAL_ITEM_STORAGE_DETAILED_AMOUNT : JadeIds.UNIVERSAL_ITEM_STORAGE_NORMAL_AMOUNT);

        if (!stacks.isEmpty()) {
            // Add a header for the items
            tooltip.append(Component.translatable("jade.items"));

            // Add items as text with their counts
            for (int i = 0; i < Math.min(max, stacks.size()); i++) {
                ItemStack item = stacks.get(i);
                Component itemName = item.getHoverName();

                if (item.getCount() > 1) {
                    tooltip.append(Component.literal("• ").append(itemName).append(Component.literal(" ×" + item.getCount())));
                } else {
                    tooltip.append(Component.literal("• ").append(itemName));
                }
            }

            // Show "and X more..." if there are more items
            if (stacks.size() > max) {
                tooltip.append(Component.translatable("jade.and_more", stacks.size() - max));
            }
        }
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        MemoryBankAccess.INSTANCE.getLoaded().ifPresent(bank -> {
            Optional<Memory> memory = bank.getMemory(accessor.getLevel(), accessor.getPosition());
            if (memory.isEmpty()) return;

            // items
            possiblyAddItems(tooltip, accessor, config, memory.get());

            Component name = memory.get().renderName();
            if (name != null) {
                tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(name));
            }
        });
    }

    @Override
    public ResourceLocation getUid() {
        return ID;
    }
}