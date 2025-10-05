
package net.kettlemc.kessentialsforge.command;
import com.mojang.brigadier.CommandDispatcher; import net.minecraft.commands.CommandSourceStack; import net.minecraft.core.BlockPos; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer; import net.kettlemc.kessentialsforge.util.DimUtil; import net.minecraft.world.phys.BlockHitResult; import net.minecraft.world.phys.HitResult;
public class TopJumpCommands {
    public static void register(CommandDispatcher<CommandSourceStack> d){
        d.register(CommandBuilders.literal("top").executes(ctx -> { ServerPlayer p=ctx.getSource().getPlayerOrException(); ServerLevel lvl=p.getLevel(); BlockPos pos=DimUtil.highestSafeY(lvl,(int)p.getX(),(int)p.getZ()); p.teleportTo(lvl,pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,p.getYHeadRot(),p.getXRot()); return 1; }));
        d.register(CommandBuilders.literal("jump").executes(ctx -> { ServerPlayer p=ctx.getSource().getPlayerOrException(); double dist=120; HitResult r=p.pick(dist,0.0F,false); if (r instanceof BlockHitResult bhr){ BlockPos pos=bhr.getBlockPos().above(); p.teleportTo(p.getLevel(), pos.getX()+0.5, pos.getY(), pos.getZ()+0.5, p.getYHeadRot(), p.getXRot()); return 1; } return 0; }));
    }
}
