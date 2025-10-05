
package net.kettlemc.kessentialsforge.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class TeleportUtil {
    public static BlockPos findSafe(ServerLevel level, BlockPos start) {
        BlockPos pos = start;
        int minY = level.getMinBuildHeight()+2;
        while (pos.getY() > minY) {
            if (level.isEmptyBlock(pos) && level.getBlockState(pos.below()).getMaterial().isSolid()) return pos;
            pos = pos.below();
        }
        return start;
    }
}
