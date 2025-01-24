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
package org.spongepowered.mod;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IEnvironmentTokenProvider;
import org.spongepowered.common.launch.SpongeLaunch;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.io.File;
import java.util.*;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class SpongeCoremod implements IFMLLoadingPlugin, IEarlyMixinLoader {

    static File modFile;

    public static final class TokenProvider implements IEnvironmentTokenProvider {

        @Override
        public int getPriority() {
            return IEnvironmentTokenProvider.DEFAULT_PRIORITY;
        }

        @Override
        public Integer getToken(String token, MixinEnvironment env) {
            if ("FORGE".equals(token)) {
                return Integer.valueOf(ForgeVersion.getBuildVersion());
            } else if ("FML".equals(token)) {
                String fmlVersion = Loader.instance().getFMLVersionString();
                int build = Integer.parseInt(fmlVersion.substring(fmlVersion.lastIndexOf('.') + 1));
                return Integer.valueOf(build);
            }
            return null;
        }

    }

    public SpongeCoremod() {
        SpongeLaunch.addJreExtensionsToClassPath();

        Launch.classLoader.addClassLoaderExclusion("org.spongepowered.common.launch.");
        Launch.classLoader.addClassLoaderExclusion("org.slf4j.");

        // Let's get this party started
        SpongeLaunch.initPaths((File) FMLInjectionData.data()[6]); // 6 = game dir
        SpongeLaunch.setupMixinEnvironment();

        // Detect dev/production env
        if (this.isProductionEnvironment()) {
            Mixins.registerErrorHandlerClass("org.spongepowered.mod.mixin.handler.MixinErrorHandler");
        }

        MixinEnvironment.getDefaultEnvironment().registerTokenProviderClass("org.spongepowered.mod.SpongeCoremod$TokenProvider");

        // Add pre-init mixins
        MixinEnvironment.getEnvironment(Phase.PREINIT).registerTokenProviderClass("org.spongepowered.mod.SpongeCoremod$TokenProvider");
        MixinEnvironment.getEnvironment(Phase.INIT).registerTokenProviderClass("org.spongepowered.mod.SpongeCoremod$TokenProvider");

        Launch.classLoader.addClassLoaderExclusion("org.spongepowered.api.event.Cancellable");
        Launch.classLoader.addClassLoaderExclusion("org.spongepowered.api.eventgencore.annotation.PropertySettings");
        Launch.classLoader.addClassLoaderExclusion("org.spongepowered.api.util.ResettableBuilder");

        // Transformer exclusions
        Launch.classLoader.addTransformerExclusion("ninja.leaping.configurate.");
        Launch.classLoader.addTransformerExclusion("org.apache.commons.lang3.");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.mod.bridge.event.EventForgeBridge");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.common.event.tracking.PhaseTracker");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.common.event.tracking.TrackingUtil");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.common.mixin.handler.TerminateVM");
        // Launch.classLoader.addTransformerExclusion("scala.");

        SpongeLaunch.setupSuperClassTransformer();

        // Setup method tracking
        //TrackerRegistry.initialize();

        // Setup IItemHandler and IItemHandlerModifiable method tracking
        //TrackerRegistry.registerTracker("org.spongepowered.mod.tracker.FluidTracker");
        //TrackerRegistry.registerTracker("org.spongepowered.mod.tracker.ItemHandlerTracker");
    }

    private boolean isProductionEnvironment() {
        return !FMLLaunchHandler.isDeobfuscatedEnvironment();
    }

    @Override
    public List<String> getMixinConfigs() {

        List<String> configs = new ArrayList<>(Arrays.asList(SpongeLaunch.getCommonMixinConfigs()));

        configs.add("mixins.forge.api.json");
        configs.add("mixins.forge.core.json");
        configs.add("mixins.forge.brokenmods.json");
        configs.add("mixins.forge.bungeecord.json");
        configs.add("mixins.forge.entityactivation.json");
        configs.add("mixins.forge.optimization.json");
        configs.add("mixins.forge.preinit.json");
        configs.add("mixins.forge.api.preinit.json");
        return configs;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[] { SpongeLaunch.SUPERCLASS_TRANSFORMER };
    }

    @Override
    public String getModContainerClass() {
        return "org.spongepowered.mod.SpongeMod";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        modFile = (File) data.get("coremodLocation");

        // Register SpongeAPI + SpongeCommon ModContainers
        FMLInjectionData.containers.add("org.spongepowered.mod.SpongeApiModContainer");
        FMLInjectionData.containers.add("org.spongepowered.mod.SpongeCommonModContainer");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
