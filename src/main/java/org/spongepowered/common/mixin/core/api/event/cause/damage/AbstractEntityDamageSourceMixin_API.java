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
package org.spongepowered.common.mixin.core.api.event.cause.damage;

import net.minecraft.entity.Entity;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.common.AbstractEntityDamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.common.event.damage.SpongeCommonEntityDamageSource;

/*
 * @author gabizou
 *
 * This is absolutely required for the abstract damage sources to have the correct
 * strings at runtime, this is the ultimate combination of the superclass class
 * transformer to force {@link AbstractDamageSource} extend {@link SpongeCommonDamageSource}
 * but still retain the sanity of the proper "damage type" for mods and native
 * Minecraft damage source.
 */
@Mixin(AbstractEntityDamageSource.class)
public abstract class AbstractEntityDamageSourceMixin_API implements EntityDamageSource {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void impl$bridgeApiToImplConstruction(final CallbackInfo callbackInfo) {
        final SpongeCommonEntityDamageSource commonSource = (SpongeCommonEntityDamageSource) (Object) this;
        commonSource.setDamageType(getType().getId());
        commonSource.setEntitySource((Entity) getSource());
        if (isAbsolute()) {
            commonSource.bridge$setDamageIsAbsolute();
        }
        if (isBypassingArmor()) {
            commonSource.bridge$setDamageBypassesArmor();
        }
        if (isExplosive()) {
            commonSource.setExplosion();
        }
        if (isMagic()) {
            commonSource.setMagicDamage();
        }
        if (isScaledByDifficulty()) {
            commonSource.setDifficultyScaled();
        }
        if (doesAffectCreative()) {
            commonSource.canHarmInCreative();
        }
    }

}
