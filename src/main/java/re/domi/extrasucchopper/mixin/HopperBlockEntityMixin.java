package re.domi.extrasucchopper.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import re.domi.extrasucchopper.HopperAccess;

import java.util.stream.IntStream;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin extends BlockEntity implements HopperAccess
{
    public HopperBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    @Unique
    private int itemPickupCooldown;

    @Override
    public int getItemPickupCooldown()
    {
        return itemPickupCooldown;
    }

    @Override
    public void setItemPickupCooldown(int itemPickupCooldown)
    {
        this.itemPickupCooldown = itemPickupCooldown;
    }

    @Shadow
    private int transferCooldown;

    @Override
    public int getTransferCooldown()
    {
        return this.transferCooldown;
    }

    @Override
    public void setTransferCooldown(int transferCooldown)
    {
        this.transferCooldown = transferCooldown;
    }

    @Shadow
    private long lastTickTime;

    @Override
    public void setLastTickTime(long lastTickTime)
    {
        this.lastTickTime = lastTickTime;
    }

    @Shadow
    private boolean isFull()
    {
        return false;
    }

    @Override
    public boolean getIsFull()
    {
        return isFull();
    }

    @Overwrite
    public static void serverTick(World world, BlockPos pos, BlockState state, HopperBlockEntity blockEntity)
    {
        if (world.isClient)
        {
            return;
        }

        HopperAccess hopper = (HopperAccess)blockEntity;
        boolean enabled = state.get(HopperBlock.ENABLED);
        int transferCooldown = hopper.getTransferCooldown() - 1;

        hopper.setLastTickTime(world.getTime());

        Inventory invAbove = null;
        boolean cachedInv = false;

        boolean hasDoneWork = false;

        if (transferCooldown <= 0)
        {
            transferCooldown = 0; // thanks mojang very epic

            if (state.get(HopperBlock.ENABLED))
            {
                if (!blockEntity.isEmpty())
                {
                    hasDoneWork = insert(world, pos, state, blockEntity);
                }

                if (!hopper.getIsFull())
                {
                    invAbove = getInputInventory(world, blockEntity);
                    cachedInv = true;

                    if (invAbove != null && !isInventoryEmpty(invAbove, Direction.DOWN))
                    {
                        final Inventory finalInvAbove = invAbove;
                        hasDoneWork |= getAvailableSlots(invAbove, Direction.DOWN).anyMatch(slot -> extract(blockEntity, finalInvAbove, slot, Direction.DOWN));
                    }
                }

                if (hasDoneWork)
                {
                    transferCooldown = 8;
                }
            }
        }

        hopper.setTransferCooldown(transferCooldown);

        int itemPickupCooldown = hopper.getItemPickupCooldown() - 1;

        if (itemPickupCooldown <= 0)
        {
            itemPickupCooldown = 0;

            if (enabled)
            {
                boolean canPickUp = cachedInv ? (invAbove == null && !hopper.getIsFull()) : (!hopper.getIsFull() && getInputInventory(world, blockEntity) == null);

                if (canPickUp && tryPickupItems(world, blockEntity))
                {
                    hasDoneWork = true;
                    itemPickupCooldown = 8;
                }
            }
        }

        hopper.setItemPickupCooldown(itemPickupCooldown);

        if (hasDoneWork)
        {
            markDirty(world, pos, state);
        }
    }

    @Unique
    private static boolean tryPickupItems(World world, Hopper hopper)
    {
        for (ItemEntity item : HopperBlockEntity.getInputItemEntities(world, hopper))
        {
            if (HopperBlockEntity.extract(hopper, item))
            {
                return true;
            }
        }

        return false;
    }

    @Shadow
    private static Inventory getInputInventory(World world, Hopper hopper)
    {
        return null;
    }

    @Shadow
    private static boolean insert(World world, BlockPos pos, BlockState state, Inventory inventory)
    {
        return false;
    }

    @Shadow
    private static boolean extract(Hopper hopper, Inventory inventory, int slot, Direction side)
    {
        return false;
    }

    @Shadow
    private static boolean isInventoryEmpty(Inventory inv, Direction facing)
    {
        return false;
    }

    @Shadow
    private static IntStream getAvailableSlots(Inventory inventory, Direction side)
    {
        return null;
    }
}
