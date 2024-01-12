package eu.pb4.polyfactory.block.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.kneelawk.graphlib.api.graph.user.BlockNode;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.advancement.FactoryTriggers;
import eu.pb4.factorytools.api.advancement.TriggerCriterion;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.block.network.NetworkComponent;
import eu.pb4.polyfactory.item.util.ColoredItem;
import eu.pb4.polyfactory.item.FactoryItems;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.polyfactory.models.CableModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.nodes.generic.SelectiveSideNode;
import eu.pb4.polyfactory.util.DyeColorExtra;
import eu.pb4.polyfactory.util.BlockStateNameProvider;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CableBlock extends NetworkBlock implements FactoryBlock, BlockEntityProvider, CableConnectable, BlockStateNameProvider, NetworkComponent.Data, NetworkComponent.Energy {
    public static final BooleanProperty NORTH;
    public static final BooleanProperty EAST;
    public static final BooleanProperty SOUTH;
    public static final BooleanProperty WEST;
    public static final BooleanProperty UP;
    public static final BooleanProperty DOWN;
    public static final Map<Direction, BooleanProperty> FACING_PROPERTIES;

    public CableBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(NORTH, false).with(SOUTH, false)
                .with(EAST, false).with(WEST, false).with(UP, false).with(DOWN, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        var stack = super.getPickStack(world, pos, state);
        if (world.getBlockEntity(pos) instanceof CableBlockEntity be && !be.isDefaultColor()) {
            ColoredItem.setColor(stack, be.getColor());
        }
        return stack;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
        var hasReceivers = false;
        var hasProviders = false;
        if (world.getBlockEntity(pos) instanceof CableBlockEntity be) {
            be.setColor(FactoryItems.CABLE.getItemColor(itemStack));

            var newState = state;
            for (var dir : Direction.values()) {
                var newPos = pos.offset(dir);
                var block = world.getBlockState(newPos);
                if (canConnectTo(world, be.getColor(), newPos, block, dir.getOpposite())) {
                    newState = newState.with(FACING_PROPERTIES.get(dir), true);
                    if (placer instanceof ServerPlayerEntity serverPlayer && (!hasReceivers || !hasProviders)) {
                        var net = NetworkComponent.Data.getLogic(serverPlayer.getServerWorld(), newPos);
                        if (net.hasReceivers()) {
                            hasReceivers = true;
                        }

                        if (net.hasProviders()) {
                            hasProviders = true;
                        }
                    }
                }
            }

            if (state != newState) {
                world.setBlockState(pos, newState);
            }
        }
        if (hasReceivers && hasProviders) {
            TriggerCriterion.trigger((ServerPlayerEntity) placer, FactoryTriggers.CABLE_CONNECT);
        }

        super.onPlaced(world, pos, state, placer, itemStack);
    }

    @Override
    protected void updateNetworkAt(WorldAccess world, BlockPos pos) {
        NetworkComponent.Data.updateDataAt(world, pos);
        NetworkComponent.Energy.updateEnergyAt(world, pos);
    }

    @Override
    protected boolean isSameNetworkType(Block block) {
        return block instanceof NetworkComponent.Data || block instanceof NetworkComponent.Energy;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var state = this.getDefaultState();
        for (var dir : Direction.values()) {
            var pos = ctx.getBlockPos().offset(dir);
            var block = ctx.getWorld().getBlockState(pos);
            if (canConnectTo(ctx.getWorld(), -1, pos, block, dir.getOpposite())) {
                state = state.with(FACING_PROPERTIES.get(dir), true);
            }
        }

        return state;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (world.getBlockEntity(pos) instanceof CableBlockEntity be) {
            return state.with(FACING_PROPERTIES.get(direction), canConnectTo(world, be.getColor(), neighborPos, neighborState, direction.getOpposite()));
        }
        return state;
    }

    private boolean canConnectTo(WorldAccess world, int ownColor, BlockPos neighborPos, BlockState neighborState, Direction direction) {
        return neighborState.getBlock() instanceof CableConnectable connectable && connectable.canCableConnect(world, ownColor, neighborPos, neighborState, direction);
    }

    @Override
    public Collection<BlockNode> createDataNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new SelectiveSideNode(this.getDirections(state)));
    }

    @Override
    public Collection<BlockNode> createEnergyNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return createDataNodes(state, world, pos);
    }

    public EnumSet<Direction> getDirections(BlockState state) {
        var list = new ArrayList<Direction>(6);

        for (var dir : Direction.values()) {
            if (state.get(FACING_PROPERTIES.get(dir))) {
                list.add(dir);
            }
        }

        return list.isEmpty() ? EnumSet.noneOf(Direction.class) : EnumSet.copyOf(list);
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.STRUCTURE_VOID;
    }

    public static int getModelId(BlockState state) {
        int i = 0;

        for(int j = 0; j < Direction.values().length; ++j) {
            if (state.get(FACING_PROPERTIES.get(Direction.values()[j]))) {
                i |= 1 << j;
            }
        }

        return i;
    }

    public static boolean hasDirection(int i, Direction direction) {
        if (direction == null) {
            return false;
        }

        return (i & (1 << direction.ordinal())) != 0;
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState);
    }

    static {
        NORTH = Properties.NORTH;
        EAST = Properties.EAST;
        SOUTH = Properties.SOUTH;
        WEST = Properties.WEST;
        UP = Properties.UP;
        DOWN = Properties.DOWN;
        FACING_PROPERTIES = ImmutableMap.copyOf((Map) Util.make(Maps.newEnumMap(Direction.class), (directions) -> {
            directions.put(Direction.NORTH, NORTH);
            directions.put(Direction.EAST, EAST);
            directions.put(Direction.SOUTH, SOUTH);
            directions.put(Direction.WEST, WEST);
            directions.put(Direction.UP, UP);
            directions.put(Direction.DOWN, DOWN);
        }));
    }

    @Override
    public boolean canCableConnect(WorldAccess world, int cableColor, BlockPos pos, BlockState state, Direction dir) {
        if (world.getBlockEntity(pos) instanceof CableBlockEntity be) {
            return be.getColor() == cableColor || be.isDefaultColor() || cableColor == 0xbbbbbb;
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public Text getName(ServerWorld world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof CableBlockEntity be && !be.isDefaultColor()) {
            if (!DyeColorExtra.hasLang(be.getColor())) {
                return Text.translatable("block.polyfactory.cable.colored.full",
                        ColoredItem.getColorName(be.getColor()), ColoredItem.getHexName(be.getColor()));
            } else {
                return Text.translatable("block.polyfactory.cable.colored", ColoredItem.getColorName(be.getColor()));
            }
        }
        return this.getName();
    }

    public static final class Model extends BlockModel {
        private final ItemDisplayElement main;
        private int color;
        private BlockState state;

        private Model(BlockState state) {
            this.main = LodItemDisplayElement.createSimple();
            this.main.setViewRange(0.5f);
            this.state = state;
            this.addElement(this.main);
        }


        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
                this.setState(this.blockState());
            }
        }

        private void setState(BlockState blockState) {
            this.state = blockState;
            updateModel();
        }

        private void updateModel() {
            var stack = CableModel.MODELS_BY_ID[getModelId(state)].copy();
            var display = new NbtCompound();
            display.putInt("color", this.color);
            stack.getOrCreateNbt().put("display", display);
            this.main.setItem(stack);
            this.tick();
        }

        public void setColor(int color) {
            this.color = color;
            updateModel();
        }
    }
}
