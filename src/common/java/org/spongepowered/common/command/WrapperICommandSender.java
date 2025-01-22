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
package org.spongepowered.common.command;

import net.minecraft.command.CommandResultStats;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.world.Locatable;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.command.CommandSourceBridge;
import org.spongepowered.common.text.SpongeTexts;
import org.spongepowered.common.util.VecHelper;

/**
 * Wrapper around a CommandSource that is not part of the base game to allow it
 * to access MC commands.
 */
public class WrapperICommandSender implements ICommandSender {

    final CommandSource source;

    private WrapperICommandSender(CommandSource source) {
        this.source = source;
    }

    @Override
    public String getName() {
        return this.source.getName();
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(this.source.getName());
    }

    @Override
    public void sendMessage(ITextComponent component) {
        this.source.sendMessage(SpongeTexts.toText(component));
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return CommandPermissions.testPermission(this.source, commandName);
    }

    @Override
    public BlockPos getPosition() {
        if (this.source instanceof Locatable) {
            return VecHelper.toBlockPos(((Locatable) this.source).getLocation());
        }
        return BlockPos.ORIGIN;
    }

    @Override
    public Vec3d getPositionVector() {
        if (this.source instanceof Locatable) {
            return VecHelper.toVec3d(((Locatable) this.source).getLocation().getPosition());
        }
        return new Vec3d(0, 0, 0);
    }

    @Override
    public World getEntityWorld() {
        if (this.source instanceof Locatable) {
            return (World) ((Locatable) this.source).getWorld();
        }
        return SpongeImpl.getServer().getEntityWorld(); // Use overworld as default
    }

    @Override
    public Entity getCommandSenderEntity() {
        if (this.source instanceof Entity) {
            return (Entity) this.source;
        }
        return null;
    }

    @Override
    public boolean sendCommandFeedback() {
        return true;
    }

    @Override
    public void setCommandStat(CommandResultStats.Type type, int amount) {

    }

    @Override
    public MinecraftServer getServer() {
        return getEntityWorld().getMinecraftServer();
    }

    public static ICommandSender of(CommandSource source) {
        if (source instanceof CommandSourceBridge) {
            return ((CommandSourceBridge) source).bridge$asICommandSender();
        }
        if (source instanceof WrapperCommandSource) {
            return ((WrapperCommandSource) source).sender;
        }
        return new WrapperICommandSender(source);
    }
}
