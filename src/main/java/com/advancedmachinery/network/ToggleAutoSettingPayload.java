package com.advancedmachinery.network;

import com.advancedmachinery.blockentity.AdvancedEmpowererBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleAutoSettingPayload(BlockPos pos, int settingType) implements CustomPacketPayload {
    public static final int AUTO_INPUT = 0;
    public static final int AUTO_OUTPUT = 1;
    public static final int ROUND_ROBIN = 2;
    public static final int SINGLE_ITEM_MODE = 3;
    public static final int SIDE_UP = 4;
    public static final int SIDE_DOWN = 5;
    public static final int SIDE_FRONT = 6;
    public static final int SIDE_BACK = 7;
    public static final int SIDE_LEFT = 8;
    public static final int SIDE_RIGHT = 9;
    public static final CustomPacketPayload.Type<ToggleAutoSettingPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("advancedmachinery", "toggle_auto_setting"));
    public static final StreamCodec<FriendlyByteBuf, ToggleAutoSettingPayload> STREAM_CODEC;

    public ToggleAutoSettingPayload(BlockPos pos, boolean isInput) {
        this(pos, isInput ? 0 : 1);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleAutoSettingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntity be = serverPlayer.level().getBlockEntity(payload.pos());
                if (be instanceof AdvancedEmpowererBlockEntity empowerer) {
                    switch (payload.settingType()) {
                        case 0 -> empowerer.toggleAutoInput();
                        case 1 -> empowerer.toggleAutoOutput();
                        case 2 -> empowerer.toggleRoundRobin();
                        case 3 -> empowerer.toggleSingleItemMode();
                        default -> {
                            if (payload.settingType() >= 4 && payload.settingType() <= 9) {
                                empowerer.cycleSideConfig(payload.settingType() - 4);
                            }
                        }
                    }
                }
            }
        });
    }

    static {
        STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ToggleAutoSettingPayload::pos,
            ByteBufCodecs.INT, ToggleAutoSettingPayload::settingType,
            ToggleAutoSettingPayload::new
        );
    }
}
