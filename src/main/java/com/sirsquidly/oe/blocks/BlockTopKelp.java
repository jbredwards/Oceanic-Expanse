package com.sirsquidly.oe.blocks;

import java.util.Random;

import com.sirsquidly.oe.Main;
import com.sirsquidly.oe.init.OEBlocks;
import com.sirsquidly.oe.init.OESounds;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockTopKelp extends BlockBush implements IGrowable, IChecksWater
{
	/** The maximum allowed height to grow to. Capped at 15 for Metadata reasons.*/
	public static int maxHeight = Math.max(Math.min(15, 15), 0);
	/** The random age given when placed. Capped to Kelp's maxHeight variable.*/
	public static int randomAge = Math.max(Math.min(14, maxHeight), 0);
	
	protected static final AxisAlignedBB KELP_TOP_AABB = new AxisAlignedBB(0.125D, 0.0D, 0.125D, 0.875D, 0.5625D, 0.875D);
	public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);
	
	public BlockTopKelp() {
		super(Material.WATER);
		this.setSoundType(OESounds.WET_GRASS);
		this.setTickRandomly(true);
		this.setCreativeTab(Main.OCEANEXPTAB);

		setDefaultState(blockState.getBaseState().withProperty(AGE, Integer.valueOf(randomAge + 1)));
	}

	@SuppressWarnings("deprecation")
	public Material getMaterial(IBlockState state)
	{
		if(Main.proxy.fluidlogged_enable) { return Material.PLANTS; }
		return super.getMaterial(state);
	}
	
	public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
		Random rand = worldIn.rand;
        return this.getDefaultState().withProperty(BlockTopKelp.AGE, Integer.valueOf(rand.nextInt(randomAge + 1)));
    }
	
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
		return KELP_TOP_AABB;
    }
    
    @Override
	public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos) {
    	if (!(worldIn.getBlockState(pos.up()).getMaterial() == Material.WATER) && !Main.proxy.fluidlogged_enable || worldIn.getBlockState(pos.up()).getBlock() == OEBlocks.KELP || worldIn.getBlockState(pos.up()).getBlock() == this) 
		{
			worldIn.setBlockState(pos, OEBlocks.KELP.getDefaultState());
		}
		this.checkAndDropBlock(worldIn, pos, state);
	}
    
	@Override
	public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        return (worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP) || worldIn.getBlockState(pos.down()).getBlock() == this || worldIn.getBlockState(pos.down()).getBlock() == OEBlocks.KELP) && checkPlaceWater(worldIn, pos, false);
    }

	@Override
	public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state)
    {
        if ((worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP)) || 
        		(worldIn.getBlockState(pos.down()).getBlock() == this) ||  
        		(worldIn.getBlockState(pos.down()).getBlock() == OEBlocks.KELP)) return true;
        return false;
    }

	@Override
	protected void checkAndDropBlock(World worldIn, BlockPos pos, IBlockState state) {
		if (!this.canBlockStay(worldIn, pos, state)) 
		{
			this.dropBlockAsItem(worldIn, pos, state, 0);
			worldIn.setBlockState(pos, Blocks.WATER.getDefaultState());
		}
	}

	@Override
	public void onBlockDestroyedByPlayer(World worldIn, BlockPos pos, IBlockState state) {
		worldIn.setBlockState(pos, Blocks.WATER.getDefaultState());
	}

	public boolean isReplaceable(IBlockAccess worldIn, BlockPos pos)
    {
        return false;
    }

	
	public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(AGE, Integer.valueOf(meta));
    }
	
	public int getMetaFromState(IBlockState state) {
		return ((Integer)state.getValue(AGE)).intValue();
	}

    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer(this, BlockLiquid.LEVEL, AGE);
    }
    
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
    {
		ItemStack itemstack = playerIn.getHeldItem(hand);
		Item item = itemstack.getItem();
		
		if ((Integer)state.getValue(AGE).intValue() != maxHeight)
        {
			if (item == Items.SHEARS)
	        {
				worldIn.setBlockState(pos, this.getDefaultState().withProperty(AGE, Integer.valueOf(maxHeight)), 2);
				worldIn.playSound((EntityPlayer)null, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
	            return true;
	        }
        }
		return false;
    }
    
    /** Natural Growing **/
    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        if (!worldIn.isAreaLoaded(pos, 1)) return;
        BlockPos blockpos = pos.up();
        int i = ((Integer)state.getValue(AGE)).intValue();
        
        if (i < 15 &&  net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, blockpos, state, rand.nextInt(1) == 0))
        {
        	this.grow(worldIn, rand, pos, state);
        }
    }
    
    /** Bonemeal Growing **/
	public boolean canGrow(World worldIn, BlockPos pos, IBlockState state, boolean isClient)
    { return ((Integer)state.getValue(AGE)).intValue() != maxHeight; }

    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state)
    { return ((Integer)state.getValue(AGE)).intValue() != maxHeight; }

    public void grow(World worldIn, Random rand, BlockPos pos, IBlockState state)
    {
    	int i = ((Integer)state.getValue(AGE)).intValue();
    	
        if (worldIn.getBlockState(pos.up()).getBlock() == Blocks.WATER && worldIn.getBlockState(pos.up(2)).getMaterial() == Material.WATER)
        {
        	worldIn.setBlockState(pos.up(), this.getDefaultState().withProperty(AGE, Integer.valueOf(i + 1)), 2);
    		worldIn.setBlockState(pos, OEBlocks.KELP.getDefaultState(), 2);
        }
        else
        {
        	worldIn.setBlockState(pos, this.getDefaultState().withProperty(AGE, Integer.valueOf(i + 1)), 2);
        }
    }
    
}
