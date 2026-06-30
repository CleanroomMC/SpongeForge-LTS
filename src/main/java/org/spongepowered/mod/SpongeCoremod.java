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
import net.minecraftforge.fml.relauncher.FMLInjectionData;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.common.launch.SpongeLaunch;
import zone.rong.mixinbooter.util.Environment;

import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class SpongeCoremod implements IFMLLoadingPlugin {

    static File modFile;

    public SpongeCoremod() {
        SpongeLaunch.addJreExtensionsToClassPath();

        Launch.classLoader.addClassLoaderExclusion("org.slf4j.");
        SpongeLaunch.initPaths((File) FMLInjectionData.data()[6]); // 6 = game dir

        // Detect dev/production env
        if (this.isProductionEnvironment()) {
            Mixins.registerErrorHandlerClass("org.spongepowered.mod.mixin.handler.MixinErrorHandler");
        }

        // NOTE: Upstream SpongeForge class-loader-excluded these API types (which mixins add as
        // interfaces/annotations to Minecraft/Forge classes) so they would load from the parent
        // class loader. That only works when the SpongeForge jar is on the system classpath. Under
        // MixinBooter 11 it is not (it is a coremod on the LaunchClassLoader only), so delegating to
        // the parent makes them UNLOADABLE: defining Forge's Event with the mixed-in Cancellable
        // interface fails with NoClassDefFoundError: org/spongepowered/api/event/Cancellable, which
        // aborts launch. We must let the LaunchClassLoader load them from the SpongeForge jar instead.
        // This is safe (no duplicate-class risk) precisely because the parent cannot load them at all,
        // and these API types are only ever used by code already running on the LaunchClassLoader.
        // Launch.classLoader.addClassLoaderExclusion("org.spongepowered.api.event.Cancellable");
        // Launch.classLoader.addClassLoaderExclusion("org.spongepowered.api.eventgencore.annotation.PropertySettings");
        // Launch.classLoader.addClassLoaderExclusion("org.spongepowered.api.util.ResettableBuilder");
        // Transformer exclusions
        Launch.classLoader.addTransformerExclusion("ninja.leaping.configurate.");
        Launch.classLoader.addTransformerExclusion("org.apache.commons.lang3.");
        // EventForgeBridge must be excluded to prevent ClassCircularityError: when Event.class is
        // being loaded (ldc Event.class in buildEvents()), CleanMix applies EventMixin_Forge which
        // loads EventMixin_Forge.class, which resolves EventForgeBridge; without this exclusion,
        // buildEvents(EventForgeBridge) would try to load Event again → ClassCircularityError.
        Launch.classLoader.addTransformerExclusion("org.spongepowered.mod.bridge.event.EventForgeBridge");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.common.event.tracking.PhaseTracker");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.common.event.tracking.TrackingUtil");
        Launch.classLoader.addTransformerExclusion("org.spongepowered.common.mixin.handler.TerminateVM");

        // MixinBooter 11 replaces the bundled SpongeMixin subsystem and registers its mixin
        // transformer (org.spongepowered.asm.mixin.transformer.Proxy) BEFORE FMLCorePlugin registers
        // its event transformers. SpongeForge mixes into net.minecraftforge.fml.common.eventhandler.Event
        // (EventMixin_Forge/EventMixin_ForgeAPI). The first load of Event is triggered by Forge's own
        // EventSubscriptionTransformer.buildEvents() doing `ldc Event.class`; the active mixin
        // transformer then intercepts Event mid-definition to apply our mixins, and the mixin
        // application re-enters buildEvents -> ldc Event.class -> ClassCircularityError, aborting launch.
        // EagerEventClassLoadTransformer forces Event to be defined (with mixins applied) in the window
        // where EventSubscriptionTransformer is being loaded but is not yet an active transformer.
        // Registered here so it is active before FMLCorePlugin's transformers are loaded.
        Launch.classLoader.addTransformerExclusion("org.spongepowered.mod.asm.transformer.EagerEventClassLoadTransformer");
        Launch.classLoader.registerTransformer("org.spongepowered.mod.asm.transformer.EagerEventClassLoadTransformer");

        SpongeLaunch.setupSuperClassTransformer();

        // Setup method tracking
        //TrackerRegistry.initialize();

        // Setup IItemHandler and IItemHandlerModifiable method tracking
        //TrackerRegistry.registerTracker("org.spongepowered.mod.tracker.FluidTracker");
        //TrackerRegistry.registerTracker("org.spongepowered.mod.tracker.ItemHandlerTracker");
    }

    private boolean isProductionEnvironment() {
        return !Environment.inDev();
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
        SpongeLaunch.setupMixinEnvironment();
        modFile = (File) data.get("coremodLocation");
        if (modFile == null) {
            modFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

}
