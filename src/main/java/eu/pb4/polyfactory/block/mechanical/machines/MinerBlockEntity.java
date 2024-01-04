package eu.pb4.polyfactory.block.mechanical.machines;

import com.mojang.authlib.GameProfile;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.mechanical.RotationUser;
import eu.pb4.factorytools.api.block.entity.LockableBlockEntity;
import eu.pb4.factorytools.api.block.OwnedBlockEntity;
import eu.pb4.polyfactory.item.FactoryItemTags;
import eu.pb4.polyfactory.ui.GuiTextures;
import eu.pb4.polyfactory.ui.TagLimitedSlot;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.factorytools.api.util.FactoryPlayer;
import eu.pb4.factorytools.api.util.VirtualDestroyStage;
import eu.pb4.polyfactory.util.inventory.SingleStackInventory;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.sgui.api.gui.SimpleGui;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.OperatorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.*;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MinerBlockEntity extends LockableBlockEntity implements SingleStackInventory, SidedInventory, OwnedBlockEntity {
    private ItemStack currentTool = ItemStack.EMPTY;
    private BlockState targetState = Blocks.AIR.getDefaultState();
    protected GameProfile owner = null;
    protected MinerPlayer player = null;
    protected double process = 0;
    private float stress = 0;
    private float lastAttackedTicks = 0;
    private MinerBlock.Model model;
    private float attackCooldownPerTick = 1;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(FactoryBlockEntities.MINER, pos, state);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("tool", this.currentTool.writeNbt(new NbtCompound()));
        nbt.putDouble("progress", this.process);
        nbt.put("block_state", NbtHelper.fromBlockState(this.targetState));
        if (this.owner != null) {
            nbt.put("owner", NbtHelper.writeGameProfile(new NbtCompound(), this.owner));
        }
        nbt.putFloat("last_attacked_ticks", this.lastAttackedTicks);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        this.currentTool = ItemStack.fromNbt(nbt.getCompound("tool"));
        this.process = nbt.getDouble("progress");
        if (nbt.contains("owner")) {
            this.owner = NbtHelper.toGameProfile(nbt.getCompound("owner"));
        }
        this.targetState = NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), nbt.getCompound("block_state"));
        this.lastAttackedTicks = nbt.getFloat("last_attacked_ticks");
        super.readNbt(nbt);
        this.updateAttackCooldownPerTick();
    }

    @Override
    public GameProfile getOwner() {
        return this.owner;
    }

    @Override
    public void setOwner(GameProfile profile) {
        this.owner = profile;
        this.markDirty();
    }

    @Override
    public ItemStack getStack() {
        return this.currentTool;
    }

    @Override
    public void setStack(ItemStack stack) {
        this.currentTool = stack;
        this.lastAttackedTicks = 0;
        this.updateAttackCooldownPerTick();
        if (this.model != null) {
            this.model.setItem(stack);
        }
    }

    private void updateAttackCooldownPerTick() {
        var baseSpeed = 4d;

        var attackSpeed = baseSpeed;
        var multiplier = new DoubleArrayList();
        var multiplier2 = new DoubleArrayList();

        for (var value : this.getStack().getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_SPEED)) {
            switch (value.getOperation()) {
                case ADDITION -> attackSpeed += value.getValue();
                case MULTIPLY_BASE -> multiplier.add(value.getValue());
                case MULTIPLY_TOTAL -> multiplier2.add(value.getValue());
            }
        }

        for(var val : multiplier) {
            attackSpeed += baseSpeed * val;
        }

        for(var val : multiplier2) {
            attackSpeed *= 1.0 + val;
        }

        this.attackCooldownPerTick = (float)(1.0 / attackSpeed * 20.0);
    }

    public float getAttackCooldownProgress() {
        return MathHelper.clamp(((float)this.lastAttackedTicks + 0.5f) / this.attackCooldownPerTick, 0.0F, 1.0F);
    }


    public MinerPlayer getFakePlayer() {
        if (this.player == null) {
            var profile = this.owner == null ? FactoryUtil.GENERIC_PROFILE : this.owner;

            this.player = new MinerPlayer(StackReference.of(this, 0), (ServerWorld) this.world, this.pos,
                    new GameProfile(profile.getId(), "Miner (" + profile.getName() + ")"));
            this.player.setPos(this.pos.getX() + 0.5, this.pos.getY() + 0.5f, this.pos.getZ() + 0.5f);
        }

        return this.player;
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return this.currentTool.isEmpty() && stack.isIn(FactoryItemTags.ALLOWED_IN_MINER);
    }

    @Override
    protected void createGui(ServerPlayerEntity playerEntity) {
        new Gui(playerEntity);
    }

    public static <T extends BlockEntity> void ticker(World world, BlockPos pos, BlockState state, T t) {
        var self = (MinerBlockEntity) t;

        if (self.model == null) {
            self.model = (MinerBlock.Model) BlockBoundAttachment.get(world, pos).holder();
            self.model.setItem(self.currentTool);
        }

        var blockPos = pos.offset(state.get(MinerBlock.FACING));
        var stateFront = world.getBlockState(blockPos);


        var entities = world.getEntitiesByClass(Entity.class, new Box(blockPos), Entity::canHit);
        if (!entities.isEmpty()) {
            var speed = Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) * MathHelper.RADIANS_PER_DEGREE * 3f;

            self.process = 0;
            world.setBlockBreakingInfo(self.getFakePlayer().getId(), blockPos, -1);
            VirtualDestroyStage.updateState(self.getFakePlayer(), blockPos, stateFront, -1);

            if (self.getAttackCooldownProgress() != 1) {
                self.lastAttackedTicks += speed;
                self.stress = 15;
                self.model.rotate((float) speed / 3);
                return;
            }
            var player = self.getFakePlayer();
            player.setLastAttackedTicks(9999999);

            var dmg = player.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
            if (dmg != null) {
                dmg.setBaseValue(0);
                dmg.clearModifiers();
                for (var x : self.getStack().getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE)) {
                    dmg.addTemporaryModifier(x);
                }
            }
            player.attack(entities.get(player.getRandom().nextInt(entities.size())));
            self.lastAttackedTicks = 0;
            return;
        }

        if (stateFront != self.targetState) {
            self.process = 0;
            self.targetState = stateFront;
            world.setBlockBreakingInfo(self.getFakePlayer().getId(), blockPos, -1);
            VirtualDestroyStage.updateState(self.getFakePlayer(), blockPos, stateFront, -1);
            return;
        }

        var player = self.getFakePlayer();

        if (self.currentTool.isEmpty() || !self.currentTool.getItem().canMine(stateFront, world, blockPos, player)) {
            self.stress = 0;
            return;
        }

        if (!CommonProtection.canBreakBlock(world, blockPos, self.owner == null ? FactoryUtil.GENERIC_PROFILE : self.owner,null)) {
            self.stress = 0;
            return;
        }

        var delta = stateFront.calcBlockBreakingDelta(player, self.world, blockPos);
        if (delta < 0) {
            delta = 0;
        }

        var speed = Math.abs(RotationUser.getRotation((ServerWorld) world, pos).speed()) * MathHelper.RADIANS_PER_DEGREE * 2.5f;

        if (stateFront.isAir() || stateFront.getOutlineShape(world, blockPos).isEmpty()) {
            self.stress = 0;
            self.model.rotate((float) speed);
            return;
        }
        self.stress = Math.min(0.2f / delta, player.canHarvest(stateFront) ? 20 : 99999);

        if (speed == 0) {
            return;
        }

        self.process += delta * speed;

        self.model.rotate((float) speed);

        var value = (int) (self.process * 10.0F);
        world.setBlockBreakingInfo(player.getId(), blockPos, value);
        VirtualDestroyStage.updateState(player, blockPos, stateFront, value);

        if (self.process >= 1) {
            self.process = 0;
            self.stress = 0;

            BlockEntity blockEntity = world.getBlockEntity(blockPos);
            if (!(stateFront.getBlock() instanceof OperatorBlock) && !player.isBlockBreakingRestricted(world, blockPos, GameMode.SURVIVAL)) {
                stateFront.getBlock().onBreak(world, blockPos, stateFront, player);
                boolean bl = world.removeBlock(blockPos, false);
                if (bl) {
                    stateFront.getBlock().onBroken(world, blockPos, stateFront);
                }

                ItemStack itemStack2 = self.currentTool.copy();
                boolean bl2 = player.canHarvest(stateFront);
                self.currentTool.postMine(world, stateFront, blockPos, player);
                if (bl && bl2) {
                    stateFront.getBlock().afterBreak(world, player, blockPos, stateFront, blockEntity, itemStack2);
                    if (self.owner != null && world.getPlayerByUuid(self.owner.getId()) instanceof ServerPlayerEntity serverPlayer) {
                        TriggerCriterion.trigger(serverPlayer, FactoryTriggers.MINER_MINES);
                    }
                }
            }
           self.markDirty();
        }
    }

    @Override
    public void markRemoved() {
        super.markRemoved();
        if (this.player != null) {
            VirtualDestroyStage.destroy(this.player);
        }
    }

    public float getStress() {
        return this.stress;
    }

    private class Gui extends SimpleGui {
        public Gui(ServerPlayerEntity player) {
            super(ScreenHandlerType.HOPPER, player, false);
            this.setTitle(GuiTextures.CENTER_SLOT_GENERIC.apply(MinerBlockEntity.this.getCachedState().getBlock().getName()));
            this.setSlotRedirect(2, new TagLimitedSlot(MinerBlockEntity.this, 0, FactoryItemTags.ALLOWED_IN_MINER));
            this.open();
        }

        @Override
        public void onClose() {
            super.onClose();
        }

        @Override
        public void onTick() {
            if (player.getPos().squaredDistanceTo(Vec3d.ofCenter(MinerBlockEntity.this.pos)) > (18 * 18)) {
                this.close();
            }
            super.onTick();
        }
    }

    public static class MinerPlayer extends FactoryPlayer {
        public MinerPlayer(StackReference toolReference, ServerWorld world, BlockPos pos, GameProfile gameProfile) {
            super(toolReference, world, pos, gameProfile);
        }

        public void setLastAttackedTicks(int tick) {
            this.lastAttackedTicks = tick;
        }


    }
}
