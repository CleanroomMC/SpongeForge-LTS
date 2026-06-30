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
package org.spongepowered.mod.plugin;

import net.minecraftforge.fml.common.ModContainer;
import org.spongepowered.api.plugin.PluginContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/**
 * Static factory for wrapping Forge {@link ModContainer} instances as Sponge
 * {@link PluginContainer} instances.
 *
 * <p>Uses a concurrent cache to ensure the same {@code ModContainer} always
 * returns the same {@code PluginContainer} adapter, which is critical for
 * identity-sensitive code such as {@code PluginManager.fromInstance()}.</p>
 */
public final class SpongePluginContainer {

    private static final Map<ModContainer, PluginContainer> CACHE = new ConcurrentHashMap<>();

    private SpongePluginContainer() {
    }

    /**
     * Wraps a {@link ModContainer} as a {@link PluginContainer}.
     *
     * @param modContainer the mod container to wrap
     * @return the cached or newly created adapter
     * @throws NullPointerException if modContainer is null
     */
    public static PluginContainer wrap(ModContainer modContainer) {
        return CACHE.computeIfAbsent(modContainer, ModContainerPluginContainerAdapter::new);
    }

    /**
     * Wraps a {@link ModContainer} as a {@link PluginContainer}, returning
     * {@code null} if the input is {@code null}.
     *
     * @param modContainer the mod container to wrap, may be null
     * @return the cached or newly created adapter, or null if input is null
     */
    @Nullable
    public static PluginContainer wrapOrNull(@Nullable ModContainer modContainer) {
        if (modContainer == null) {
            return null;
        }
        return wrap(modContainer);
    }
}