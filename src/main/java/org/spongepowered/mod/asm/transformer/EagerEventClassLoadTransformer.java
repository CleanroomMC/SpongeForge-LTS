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
package org.spongepowered.mod.asm.transformer;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Works around a launch-time {@code ClassCircularityError} on
 * {@code net.minecraftforge.fml.common.eventhandler.Event} that occurs under
 * MixinBooter 11.
 *
 * <p>SpongeForge mixes into Forge's {@code Event} base class
 * ({@code EventMixin_Forge} adds {@code EventForgeBridge};
 * {@code EventMixin_ForgeAPI} adds {@code Cancellable}). MixinBooter registers
 * its mixin transformer before FMLCorePlugin registers Forge's event
 * transformers. The very first load of {@code Event} is triggered by Forge's
 * own {@code EventSubscriptionTransformer.buildEvents()} executing
 * {@code ldc Event.class}; the active mixin transformer then intercepts
 * {@code Event} mid-definition to apply our mixins, and that application
 * re-enters {@code buildEvents} -> {@code ldc Event.class} while {@code Event}
 * is still being defined -> {@code ClassCircularityError}, aborting launch.</p>
 *
 * <p>The fix is to force {@code Event} to be fully defined (with our mixins
 * applied) in a window where no {@code buildEvents} call is on the stack. When
 * the {@code $wrapper.EventSubscriptionTransformer} class is being loaded its
 * backing {@code EventSubscriptionTransformer} has not yet been added to the
 * active transformer chain, so loading {@code Event} here cannot re-enter
 * {@code buildEvents}. By that point the Sponge mixin configs have already been
 * prepared, so the Event mixins apply correctly.</p>
 *
 * <p>MixinBooter ships an equivalent fix ({@code EagerlyLoadEventClassTransformer})
 * but never registers it, and it constructs a full {@code Event} instance; we
 * only need the class defined, so a load (without initialisation) is enough and
 * avoids running {@code Event}'s constructor. Any failure is logged here rather
 * than escaping as a silent {@link Error} that LaunchWrapper does not catch.</p>
 */
public final class EagerEventClassLoadTransformer implements IClassTransformer {

    private static final String WRAPPER_NAME =
            "$wrapper.net.minecraftforge.fml.common.asm.transformers.EventSubscriptionTransformer";
    private static final String FORGE_EVENT = "net.minecraftforge.fml.common.eventhandler.Event";

    private boolean done;

    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] basicClass) {
        if (!this.done && WRAPPER_NAME.equals(name)) {
            this.done = true;
            final Logger log = LogManager.getLogger("Sponge");
            try {
                // initialize = false: define + link Event (applies our mixins) without running its
                // constructor/static initialiser. This is all that is needed to keep Event out of the
                // "currently being defined" state when buildEvents later does ldc Event.class.
                Class.forName(FORGE_EVENT, false, Launch.classLoader);
                log.info("[SpongeForge] Eagerly loaded {} to avoid ClassCircularityError under MixinBooter 11.", FORGE_EVENT);
            } catch (final Throwable t) {
                log.fatal("[SpongeForge] Failed to eagerly load {} (Event mixins could not be applied cleanly). "
                        + "This is the underlying launch failure:", FORGE_EVENT, t);
                // Mirror to stderr in case the log4j appenders are not flushed before the VM dies.
                t.printStackTrace();
            }
        }
        return basicClass;
    }
}
