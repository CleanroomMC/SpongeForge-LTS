/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.event.tracking.phase.packet.player;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.world.World;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.OwnershipTrackedBridge;
import org.spongepowered.common.event.tracking.TrackingUtil;
import org.spongepowered.common.event.tracking.context.ItemDropData;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketContext;
import org.spongepowered.common.event.tracking.phase.packet.BasicPacketState;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public final class AttackEntityPacketState extends BasicPacketState {

    private BiConsumer<CauseStackManager.StackFrame, BasicPacketContext>
        ATTACK_MODIFIER = super.getFrameModifier().andThen((frame, ctx) -> {
        frame.addContext(EventContextKeys.USED_ITEM, ctx.getItemUsedSnapshot());
        frame.addContext(EventContextKeys.USED_HAND, ctx.getHandUsed());
    });

    @Override
    public BiConsumer<CauseStackManager.StackFrame, BasicPacketContext> getFrameModifier() {
        return this.ATTACK_MODIFIER;
    }

    @Override
    public boolean isPacketIgnored(Packet<?> packetIn, EntityPlayerMP packetPlayer) {
        final CPacketUseEntity useEntityPacket = (CPacketUseEntity) packetIn;
        // There are cases where a player is interacting with an entity that
        // doesn't exist on the server.
        @Nullable
        net.minecraft.entity.Entity entity = useEntityPacket.getEntityFromWorld(packetPlayer.world);
        return entity == null;
    }

    @Override
    public void populateContext(EntityPlayerMP playerMP, Packet<?> packet, BasicPacketContext context) {
        context.itemUsed(ItemStackUtil.cloneDefensive(playerMP.getHeldItemMainhand()))
            .handUsed(HandTypes.MAIN_HAND);
    }


    @Override
    public void unwind(BasicPacketContext context) {
        final EntityPlayerMP player = context.getPacketPlayer();
        final CPacketUseEntity useEntityPacket = context.getPacket();
        final net.minecraft.entity.Entity entity = useEntityPacket.getEntityFromWorld(player.world);
        if (entity == null) {
            // Something happened?
            return;
        }
        final World spongeWorld = (World) player.world;
        if (entity instanceof OwnershipTrackedBridge) {
            ((OwnershipTrackedBridge) entity).tracked$setOwnerReference((User) player);
        } else {
            ((Entity) entity).setNotifier(player.getUniqueID());
        }

        context.getCapturedItemsSupplier()
            .acceptAndClearIfNotEmpty(items -> {
                // For destruction, this should be empty, however, some
                // times,
                // it may not be?
                final PrettyPrinter printer = new PrettyPrinter(60);
                printer.add("Processing Attack Entity").centre().hr();
                printer.add("There are some captured items after the entity was destructed!");
                printer.addWrapped(60, "%s : %s", "Items captured", items);
                printer.add("Stacktrace:");
                printer.add(new Exception("Stack trace"));
                printer.trace(System.err, SpongeImpl.getLogger(), Level.TRACE);
            });
        // TODO - Determine if we need to pass the supplier or perform some parameterized
        //  process if not empty method on the capture object.
        TrackingUtil.processBlockCaptures(context);
        context.getPerEntityItemDropSupplier().acceptAndClearIfNotEmpty(map -> {
            for (Map.Entry<UUID, Collection<ItemDropData>> entry : map.asMap().entrySet()) {
                final UUID key = entry.getKey();
                final Optional<Entity> affectedEntity = spongeWorld.getEntity(key);
                if (!affectedEntity.isPresent()) {
                    continue;
                }
                final Collection<ItemDropData> itemStacks = entry.getValue();
                if (itemStacks.isEmpty()) {
                    return;
                }
                final List<ItemDropData> items = new ArrayList<>(itemStacks);

                if (!items.isEmpty()) {
                    try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                        final List<Entity> itemEntities = items.stream()
                            .map(data -> data.create(((WorldServer) player.world)))
                            .map(entity1 -> (Entity) entity1)
                            .collect(Collectors.toList());
                        frame.pushCause(player);
                        frame.pushCause(affectedEntity.get());
                        frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);

                        final DropItemEvent.Destruct destruct =
                            SpongeEventFactory.createDropItemEventDestruct(frame.getCurrentCause(), itemEntities);
                        SpongeImpl.postEvent(destruct);
                        if (!destruct.isCancelled()) {
                            processSpawnedEntities(player, destruct);
                        }
                    }
                }
            }
        });
        context.getPerEntityItemEntityDropSupplier().acceptAndClearIfNotEmpty(map -> {
            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                frame.pushCause(player);
                frame.addContext(EventContextKeys.SPAWN_TYPE, SpawnTypes.DROPPED_ITEM);
                for (Map.Entry<UUID, Collection<EntityItem>> entry : map.asMap().entrySet()) {
                    final UUID key = entry.getKey();
                    final Optional<Entity> attackedEntities = spongeWorld.getEntity(key);
                    if (!attackedEntities.isPresent()) {
                        continue;
                    }
                    final List<Entity> items = entry.getValue().stream().map(entity1 -> (Entity) entity1).collect(Collectors.toList());

                    final DropItemEvent.Destruct destruct =
                        SpongeEventFactory.createDropItemEventDestruct(frame.getCurrentCause(), items);
                    SpongeImpl.postEvent(destruct);
                    if (!destruct.isCancelled()) {
                        processSpawnedEntities(player, destruct);
                    }
                }
            }
        });
    }

    @Override
    public boolean tracksEntitySpecificDrops() {
        return true;
    }
}
