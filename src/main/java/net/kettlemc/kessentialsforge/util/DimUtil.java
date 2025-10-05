
package net.kettlemc.kessentialsforge.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class DimUtil {
    public static ServerLevel levelById(net.minecraft.server.MinecraftServer server, String dimId) {
        ResourceKey<Level> key = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimId));
        return server.getLevel(key);
    }
    public static BlockPos highestSafeY(ServerLevel level, int x, int z) {
        int y = level.getMaxBuildHeight() - 1;
        while (y > level.getMinBuildHeight()) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).getMaterial().isSolid()) return pos;
            y--;
        }
        return new BlockPos(x, level.getMinBuildHeight()+2, z);
    }
}
