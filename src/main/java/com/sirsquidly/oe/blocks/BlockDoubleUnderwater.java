package com.sirsquidly.oe.blocks;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import com.sirsquidly.oe.Main;
import com.sirsquidly.oe.init.OEBlocks;
import com.sirsquidly.oe.init.OESounds;
import com.sirsquidly.oe.util.handlers.ConfigHandler;

public class BlockDoubleUnderwater extends BlockBush implements IChecksWater
{
	protected static final AxisAlignedBB TALL_SEAGRASS_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 1.0D, 0.875D);
	public static final PropertyEnum<BlockDoubleUnderwater.EnumBlockHalf> HALF = PropertyEnum.<BlockDoubleUnderwater.EnumBlockHalf>create("half", BlockDoubleUnderwater.EnumBlockHalf.class);
	
	public BlockDoubleUnderwater() {
		super(Material.WATER);
		this.setSoundType(OESounds.WET_GRASS);

		setDefaultState(blockState.getBaseState().withProperty(HALF, BlockDoubleUnderwater.EnumBlockHalf.LOWER));
	}
	
	@SuppressWarnings("deprecation")
	public Material getMaterial(IBlockState state)
	{
		 if(Main.proxy.fluidlogged_enable) { return Material.PLANTS; }
		return super.getMaterial(state);
	}
	 
	@Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        return TALL_SEAGRASS_AABB;
    }
	
	@Override
	public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        return worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP) && checkPlaceWater(worldIn, pos, false) && checkPlaceWater(worldIn, pos.up(), true);
    }
	
	@Override
	public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state)
    {
        if (state.getBlock() != this) return super.canBlockStay(worldIn, pos, state); //Forge: This function is called during world gen and placement, before this block is set, so if we are not 'here' then assume it's the pre-check.
        if (state.getValue(HALF) == BlockDoubleUnderwater.EnumBlockHalf.UPPER)
        {
        	if (!checkWater(worldIn, pos) && !Main.proxy.fluidlogged_enable) return false;
            return worldIn.getBlockState(pos.down()).getBlock() == this;
        }
        else
        {
            IBlockState iblockstate = worldIn.getBlockState(pos.up());
            if (iblockstate.getBlock() != this) return false;
            return worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP);
        }
    }

	public void placeAt(World worldIn, BlockPos lowerPos, int flags)
    {
        worldIn.setBlockState(lowerPos, this.getDefaultState().withProperty(HALF, BlockDoubleUnderwater.EnumBlockHalf.LOWER), flags);
        worldIn.setBlockState(lowerPos.up(), this.getDefaultState().withProperty(HALF, BlockDoubleUnderwater.EnumBlockHalf.UPPER), flags);
    }

	// Just used the placeAt, less typing
    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
    {
    	this.placeAt(worldIn, pos, 2);
    }
	
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return meta == 0 ? this.getDefaultState().withProperty(HALF, BlockDoubleUnderwater.EnumBlockHalf.UPPER) : this.getDefaultState().withProperty(HALF, BlockDoubleUnderwater.EnumBlockHalf.LOWER);
    }

    @Override
    public int getMetaFromState(IBlockState state)
    {
        return state.getValue(HALF) == BlockDoubleUnderwater.EnumBlockHalf.UPPER ? 0 : 1;
    }
    
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, new IProperty[] {HALF, BlockLiquid.LEVEL});
    }

    protected void checkAndDropBlock(World worldIn, BlockPos pos, IBlockState state)
    {
        if (!this.canBlockStay(worldIn, pos, state))
        {
            boolean flag = state.getValue(HALF) == BlockDoubleUnderwater.EnumBlockHalf.UPPER;
            BlockPos blockpos = flag ? pos : pos.up();
            BlockPos blockpos1 = flag ? pos.down() : pos;
            Block block = (Block)(flag ? this : worldIn.getBlockState(blockpos).getBlock());
            Block block1 = (Block)(flag ? worldIn.getBlockState(blockpos1).getBlock() : this);
            
            if (block == this)
            {
                worldIn.setBlockState(blockpos, Blocks.WATER.getDefaultState(), 2);
            }

            if (block1 == this)
            {
                worldIn.setBlockState(blockpos1, Blocks.WATER.getDefaultState(), 3);
            }
        }
    }
    
    /**
     * Shearing stuffs.
     */
	public boolean isShearable(ItemStack item, IBlockAccess world, BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        return state.getValue(HALF) == EnumBlockHalf.LOWER;
    }
    public java.util.List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune)
    {
        return java.util.Arrays.asList(new ItemStack(OEBlocks.SEAGRASS, 2));
    }
    
    @Override
	public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack)
    {
        if (!worldIn.isRemote && stack.getItem() == Items.SHEARS && ConfigHandler.block.seagrass.enableSeagrass)
        {
            player.addStat(StatList.getBlockStats(this));
            spawnAsEntity(worldIn, pos, new ItemStack(OEBlocks.SEAGRASS, 2, 0));
        }
        else
        {
            super.harvestBlock(worldIn, player, pos, state, te, stack);
        }
    }

	public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return Items.AIR;
    }
	
	public static enum EnumBlockHalf implements IStringSerializable
    {
        UPPER,
        LOWER;

        public String toString()
        {
            return this.getName();
        }

        public String getName()
        {
            return this == UPPER ? "upper" : "lower";
        }
    }
}
