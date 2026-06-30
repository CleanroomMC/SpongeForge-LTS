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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.emptyToNull;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.plugin.meta.PluginDependency;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Adapter that wraps a Forge {@link ModContainer} as a Sponge {@link PluginContainer}.
 *
 * <p>This replaces the {@code ModContainerMixin_ForgeAPI} interface mixin which could not be
 * applied because {@code ModContainer} is loaded by Forge's {@code PatchingTransformer} before
 * any mixin phase runs.</p>
 */
public final class ModContainerPluginContainerAdapter implements PluginContainer {

    private final ModContainer modContainer;

    ModContainerPluginContainerAdapter(ModContainer modContainer) {
        this.modContainer = checkNotNull(modContainer, "modContainer");
    }

    @Override
    public String getId() {
        return checkNotNull(emptyToNull(this.modContainer.getModId()), "modid");
    }

    @Override
    public Optional<String> getVersion() {
        String version = emptyToNull(this.modContainer.getVersion());
        if (version != null && ("unknown".equalsIgnoreCase(version) || "dev".equalsIgnoreCase(version))) {
            version = null;
        }
        return Optional.ofNullable(version);
    }

    @Override
    public Optional<String> getDescription() {
        final ModMetadata meta = this.modContainer.getMetadata();
        return meta != null ? Optional.ofNullable(emptyToNull(meta.description)) : Optional.empty();
    }

    @Override
    public Optional<String> getUrl() {
        final ModMetadata meta = this.modContainer.getMetadata();
        return meta != null ? Optional.ofNullable(emptyToNull(meta.url)) : Optional.empty();
    }

    @Override
    public List<String> getAuthors() {
        final ModMetadata meta = this.modContainer.getMetadata();
        return meta != null ? ImmutableList.copyOf(meta.authorList) : ImmutableList.of();
    }

    @Override
    public Set<PluginDependency> getDependencies() {
        return DependencyHandler.collectDependencies(this.modContainer);
    }

    @Override
    public Optional<PluginDependency> getDependency(String id) {
        return Optional.ofNullable(DependencyHandler.findDependency(this.modContainer, id));
    }

    @Override
    public Optional<Path> getSource() {
        final File source = this.modContainer.getSource();
        if (source != null) {
            return Optional.of(source.toPath());
        }
        return Optional.empty();
    }

    @Override
    public Optional<?> getInstance() {
        return Optional.ofNullable(this.modContainer.getMod());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ModContainerPluginContainerAdapter) {
            return this.modContainer.equals(((ModContainerPluginContainerAdapter) obj).modContainer);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.modContainer.hashCode();
    }

    @Override
    public String toString() {
        return "ModContainerPluginContainerAdapter{modContainer=" + this.modContainer.getModId() + "}";
    }
}