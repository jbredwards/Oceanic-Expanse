package com.sirsquidly.oe.entity;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.sirsquidly.oe.entity.ai.EntityAIWanderUnderwater;
import com.sirsquidly.oe.init.OEBlocks;
import com.sirsquidly.oe.util.handlers.LootTableHandler;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIFollowParent;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMate;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

public class EntityTurtle extends AbstractFish
{
	private static final Set<Item>BREEDING_ITEMS = Sets.newHashSet(Item.getItemFromBlock(OEBlocks.SEAGRASS));
	private static final UUID SWIMMING_SPEED_BOOST_ID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
	private static final AttributeModifier SWIM_SPEED_BOOST = (new AttributeModifier(SWIMMING_SPEED_BOOST_ID, "Swimming speed boost", 0.15000000596046448D, 0)).setSaved(false);
	protected static final DataParameter<BlockPos> HOME_BLOCK_POS = EntityDataManager.<BlockPos>createKey(EntityTurtle.class, DataSerializers.BLOCK_POS);
	private static final DataParameter<Boolean> CARRYING_EGG = EntityDataManager.<Boolean>createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> GOIN_HOME = EntityDataManager.<Boolean>createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
	private final PathNavigateSwimmer waterNavigator;
	private final PathNavigateGround groundNavigator;
		    
	public EntityTurtle(World worldIn) 
	{
		super(worldIn);
		this.setSize(1.4F, 0.55F);
        stepHeight = 1.0F;
        this.setPathPriority(PathNodeType.WALKABLE, 1.0F);
		this.setPathPriority(PathNodeType.WATER, 0.0F);
		
        this.waterNavigator = new PathNavigateSwimmer(this, worldIn);
        this.groundNavigator = new PathNavigateGround(this, worldIn);
        this.setHomePos(new BlockPos(this));
        this.rand.setSeed((long)(1 + this.getEntityId()));
	}
	
	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(HOME_BLOCK_POS, BlockPos.ORIGIN);
		this.dataManager.register(CARRYING_EGG, Boolean.valueOf(false));
		this.dataManager.register(GOIN_HOME, Boolean.valueOf(false));
	}
	
	@Override
	public boolean canBeLeashedTo(EntityPlayer player)
    { return false; }
	
	@Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData livingdata)
    {
		livingdata = super.onInitialSpawn(difficulty, livingdata);
        this.setHomePos(new BlockPos(this));
        return livingdata;
    }
    
	@Override
	protected void applyEntityAttributes()
    {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.08D);
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(3.0D);
    }
	
	protected void initEntityAI()
    {	
        this.tasks.addTask(1, new EntityTurtle.TurtleAIMate(this, 1.0D));
        this.tasks.addTask(2, new EntityTurtle.TurtleAIGOHOME(this, 1.0D));
        this.tasks.addTask(3, new EntityAIPanic(this, 1.1D));
        this.tasks.addTask(3, new EntityAIFollowParent(this, 1.1D));
        this.tasks.addTask(3, new EntityTurtle.TurtleAITempt(this, 1.1D, BREEDING_ITEMS));
        this.tasks.addTask(5, new EntityTurtle.EntityAIWanderLand(this, 1.0D, 40));
		this.tasks.addTask(5, new EntityAIWanderUnderwater(this, 1.0D, 80));
		this.tasks.addTask(6, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(6, new EntityAILookIdle(this));
    }
	
	@Override
    protected ResourceLocation getLootTable()
    { return LootTableHandler.ENTITIES_TURTLE; }
	
	public void onStruckByLightning(EntityLightningBolt lightningBolt)
    {
        if (!this.world.isRemote)
        {
        	
        	ItemStack itemstack = new ItemStack(Items.BOWL, 1);

            if (this.hasCustomName())
            {
                itemstack.setStackDisplayName(this.getCustomNameTag());
            }

            this.entityDropItem(itemstack, 0.0F);
        	this.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 9999.0F);
        }
    }
	
	public EntityTurtle createChild(EntityAgeable ageable)
    { 
		EntityTurtle entityturtle = new EntityTurtle(this.world);
		entityturtle.setHomePos(new BlockPos(entityturtle));
		
        return entityturtle;
    }
	
	@Override
	public void setScaleForAge(boolean child)
    { this.setScale(child ? 0.25F : 1.0F); }

	public boolean isBreedingItem(ItemStack stack)
    { return BREEDING_ITEMS.contains(stack.getItem()); }
	
	public float getEyeHeight()
    { return this.height * 0.5F; }
	
	public boolean isFlopping() { return false; }
	
	@Override
	public void onEntityUpdate() 
	{
        super.onEntityUpdate();

        IAttributeInstance iattributeinstance = this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        
        if (!world.isRemote) {
            if (isServerWorld() && isInWater()) 
            { 
            	navigator = waterNavigator; 
            	if (!iattributeinstance.hasModifier(SWIM_SPEED_BOOST))
                { iattributeinstance.applyModifier(SWIM_SPEED_BOOST); }
            }
            else 
            { 
            	navigator = groundNavigator;
            	if (iattributeinstance.hasModifier(SWIM_SPEED_BOOST))
            	{ iattributeinstance.removeModifier(SWIM_SPEED_BOOST); }
            }
        }
    }
	
	
	public void setHomePos(BlockPos pos)
    { this.dataManager.set(HOME_BLOCK_POS, pos); }
	
	public BlockPos getHomePos()
    { return (BlockPos)this.dataManager.get(HOME_BLOCK_POS); }
	
	public boolean isGoingHome()
    { return ((Boolean)this.dataManager.get(GOIN_HOME)).booleanValue(); }

    public void setGoingHome(boolean bool)
    { this.dataManager.set(GOIN_HOME, Boolean.valueOf(bool)); }
    
    public boolean isCarryingEgg()
    { return ((Boolean)this.dataManager.get(CARRYING_EGG)).booleanValue(); }

    public void setCarryingEgg(boolean bool)
    { this.dataManager.set(CARRYING_EGG, Boolean.valueOf(bool)); }
    
    public class TurtleAIMate extends EntityAIMate
	{
    	private final EntityTurtle turtle;
    	World world;
    	private EntityTurtle targetMate;
    	int spawnBabyDelay;
    	double moveSpeed;
        
    	public TurtleAIMate(EntityTurtle turtle, double speedIn)
        {
            super(turtle, speedIn);
            this.turtle = turtle;
            this.world = turtle.world;
            this.moveSpeed = speedIn;
        }
    	
    	public boolean shouldExecute()
        {
            if (this.turtle.isCarryingEgg())
            { return false; }
            
            this.targetMate = this.getNearbyMate();
            return this.targetMate != null;
        }
    	
    	public boolean shouldContinueExecuting()
    	{ return this.targetMate.isEntityAlive() && this.targetMate.isInLove() && this.spawnBabyDelay < 60; }
    	 
    	private EntityTurtle getNearbyMate()
        {
            List<EntityTurtle> list = this.world.<EntityTurtle>getEntitiesWithinAABB(EntityTurtle.class, this.turtle.getEntityBoundingBox().grow(8.0D));
            double d0 = Double.MAX_VALUE;
            EntityTurtle nearbyTurtle = null;

            for (EntityTurtle nearbyTurtle1 : list)
            {
                if (this.turtle.canMateWith(nearbyTurtle1) && this.turtle.getDistanceSq(nearbyTurtle1) < d0)
                {
                	nearbyTurtle = nearbyTurtle1;
                    d0 = this.turtle.getDistanceSq(nearbyTurtle1);
                }
            }

            return nearbyTurtle;
        }
    	
    	public void updateTask()
        {
            this.turtle.getLookHelper().setLookPositionWithEntity(this.targetMate, 10.0F, (float)this.turtle.getVerticalFaceSpeed());
            this.turtle.getNavigator().tryMoveToEntityLiving(this.targetMate, this.moveSpeed);
            ++this.spawnBabyDelay;

            if (this.spawnBabyDelay >= 60 && this.turtle.getDistanceSq(this.targetMate) < 9.0D)
            {
            	this.turtle.setGrowingAge(6000);
                this.turtle.resetInLove();
                this.targetMate.resetInLove();
                
                final net.minecraftforge.event.entity.living.BabyEntitySpawnEvent event = new net.minecraftforge.event.entity.living.BabyEntitySpawnEvent(turtle, targetMate, null);
                final boolean cancelled = net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                if (cancelled) { return; }
                
                EntityPlayerMP playerMP = this.turtle.getLoveCause();

                if (playerMP == null && this.targetMate.getLoveCause() != null)
                { playerMP = this.targetMate.getLoveCause(); }

                if (playerMP != null)
                {
                	playerMP.addStat(StatList.ANIMALS_BRED);
                    CriteriaTriggers.BRED_ANIMALS.trigger(playerMP, this.turtle, this.targetMate, null);
                }
                
                this.turtle.setGoingHome(true);
                this.turtle.setCarryingEgg(true);
                Random random = this.turtle.getRNG();

                if (this.world.getGameRules().getBoolean("doMobLoot"))
                { this.world.spawnEntity(new EntityXPOrb(this.world, this.turtle.posX, this.turtle.posY, this.turtle.posZ, random.nextInt(7) + 1)); }
            }
        }

	}
    
	@SuppressWarnings("unused")
	public class TurtleAITempt extends EntityAIBase
	{
		private final EntityCreature turtle;
	    private final double speed;
		private double targetX;
		private double targetY;
		private double targetZ;
	    private EntityPlayer temptPlay;
	    private int delay;
	    private final Set<Item> temptItem;
	    
		public TurtleAITempt(EntityCreature temptedEntityIn, double speedIn, Item temptItemIn)
	    { this(temptedEntityIn, speedIn, Sets.newHashSet(temptItemIn)); }

	    public TurtleAITempt(EntityCreature temptedEntityIn, double speedIn, Set<Item> temptItemIn)
	    {
	        this.turtle = temptedEntityIn;
	        this.speed = speedIn;
	        this.temptItem = temptItemIn;
	        this.setMutexBits(3);
	    }

	    public boolean shouldExecute()
	    {
	        if (this.delay > 0) { --this.delay; return false; }
	        else
	        {
	            this.temptPlay = this.turtle.world.getClosestPlayerToEntity(this.turtle, 10.0D);

	            if (this.temptPlay == null) { return false; }
	            else
	            { return this.isTempting(this.temptPlay.getHeldItemMainhand()) || this.isTempting(this.temptPlay.getHeldItemOffhand()); }
	        }
	    }

	    protected boolean isTempting(ItemStack stack)
	    { return this.temptItem.contains(stack.getItem()); }

	    public void startExecuting()
	    {
	        this.targetX = this.temptPlay.posX;
	        this.targetY = this.temptPlay.posY;
	        this.targetZ = this.temptPlay.posZ;
	    }

	    public void resetTask()
	    {
	        this.temptPlay = null;
	        this.turtle.getNavigator().clearPath();
	        this.delay = 100;
	    }

	    public void updateTask()
	    {
	        this.turtle.getLookHelper().setLookPositionWithEntity(this.temptPlay, (float)(this.turtle.getHorizontalFaceSpeed() + 20), (float)this.turtle.getVerticalFaceSpeed());

	        if (this.turtle.getDistanceSq(this.temptPlay) < 6.25D)
	        { this.turtle.getNavigator().clearPath(); }
	        else
	        { this.turtle.getNavigator().tryMoveToEntityLiving(this.temptPlay, this.speed); }
	    }
		
	}
	
	public class EntityAIWanderLand extends EntityAIWander
	{
		public EntityAIWanderLand(EntityCreature creatureIn, double speedIn, int chance) 
		{
			super(creatureIn, 1.0, chance);
		}
		
		@Override
		public boolean shouldExecute()
	    {
	        if (this.entity.isInWater() || ((EntityTurtle) this.entity).isGoingHome())
	        { return false; }

	        return super.shouldExecute();
	    }
	}
	
	public class TurtleAIGOHOME extends EntityAIBase
	{
		private final EntityTurtle turtle;
	    private final double speed;
		private double moveX;
		private double moveY;
		private double moveZ;
	    private int giveUp;

	    public TurtleAIGOHOME(EntityTurtle turtleIn, double speedIn)
	    {
	        this.turtle = turtleIn;
	        this.speed = speedIn;
	        this.setMutexBits(1);
	    }

	    public boolean shouldExecute()
	    {
	    	if (this.turtle.isGoingHome())
	    	{ return true; }
	    	if (this.turtle.getRNG().nextInt(1000) != 0 || this.turtle.isChild())
	    	{ return false; }
	    	if (this.turtle.getDistanceSqToCenter(this.turtle.getHomePos()) > 5.0D)
	    	{ return true; }
	    	
	    	return false;
	    }
	    
	    public void startExecuting()
	    { this.turtle.setGoingHome(true); this.giveUp = 100; }

	    public void resetTask()
	    {
	        this.giveUp = 0;
	    }

	    public boolean shouldContinueExecuting()
	    { return this.giveUp > 0;}
	    
	    public void updateTask()
	    {
	    	if (this.turtle.getDistanceSqToCenter(this.turtle.getHomePos()) < 16.0D)
	    	{ --this.giveUp; }
	    	if (this.turtle.getDistanceSqToCenter(this.turtle.getHomePos()) < 3.0D)
	    	{ --this.giveUp; --this.giveUp; --this.giveUp;}
	    	
	    	BlockPos turtlePos = new BlockPos(this.turtle.posX, this.turtle.posY, this.turtle.posZ);
	    	
	    	BlockPos blockpos = this.turtle.getHomePos();
            Vec3d vec3d = RandomPositionGenerator.findRandomTargetBlockTowards(this.turtle, 16, 7, new Vec3d(blockpos.getX(), blockpos.getY(), blockpos.getZ()));
            
            if (this.turtle.world.getBlockState(turtlePos.up()).getMaterial() == Material.AIR && this.turtle.world.getBlockState(turtlePos).getMaterial() == Material.WATER && this.turtle.world.getBlockState(turtlePos.down()).getMaterial() != Material.WATER)
            { this.turtle.motionY -= 0.1D; }
            
            if (this.turtle.getNavigator().noPath() && vec3d != null)
        	{
            	
            	if (this.turtle.getDistanceSqToCenter(this.turtle.getHomePos()) > 16.0D)
            	{
            		this.moveX = vec3d.x;
            		this.moveY = vec3d.y;
            		this.moveZ = vec3d.z;
            	}
            	else
    	    	{ this.turtle.getNavigator().tryMoveToXYZ(blockpos.getX(), blockpos.getY(), blockpos.getZ(), this.speed);; }
        	}	
	        this.turtle.getNavigator().tryMoveToXYZ(this.moveX, this.moveY, this.moveZ, this.speed);
	        
	        if (this.giveUp <= 1)
	        { this.turtle.setGoingHome(false); }
	    }
		
	}
	
	public void writeEntityToNBT(NBTTagCompound compound)
    {
        super.writeEntityToNBT(compound);
        
        compound.setBoolean("Going_Home", this.isGoingHome());
        compound.setBoolean("Has_Egg", this.isCarryingEgg());
        BlockPos blockpos = this.getHomePos();
        compound.setInteger("HomePosX", blockpos.getX());
        compound.setInteger("HomePosY", blockpos.getY());
        compound.setInteger("HomePosZ", blockpos.getZ());
    }

    public void readEntityFromNBT(NBTTagCompound compound)
    {
        super.readEntityFromNBT(compound);
        
        this.setGoingHome(compound.getBoolean("Going_Home"));
        this.setCarryingEgg(compound.getBoolean("Has_Egg"));
        int x = compound.getInteger("HomePosX");
        int y = compound.getInteger("HomePosY");
        int z = compound.getInteger("HomePosZ");
        this.dataManager.set(HOME_BLOCK_POS, new BlockPos(x, y, z));
    }
    
    static class GroupData implements IEntityLivingData
    {
        public BlockPos homePos;

        private GroupData(BlockPos homeIn)
        {
            this.homePos = homeIn;
        }
    }
}