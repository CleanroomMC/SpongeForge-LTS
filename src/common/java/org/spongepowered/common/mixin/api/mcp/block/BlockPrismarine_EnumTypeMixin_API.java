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
package org.spongepowered.common.mixin.api.mcp.block;

import net.minecraft.block.BlockPrismarine;
import org.spongepowered.api.data.type.PrismarineType;
import org.spongepowered.api.text.translation.Translation;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.text.translation.SpongeTranslation;

import javax.annotation.Nullable;

@Mixin(BlockPrismarine.EnumType.class)
@Implements(@Interface(iface = PrismarineType.class, prefix = "prismarine$"))
public abstract class BlockPrismarine_EnumTypeMixin_API {

    @Shadow public abstract String shadow$getTranslationKey();

    @Nullable private Translation api$translation;

    public String prismarine$getId() {
        return "minecraft:" + shadow$getTranslationKey();
    }

    @Intrinsic
    public String prismarine$getName() {
        return shadow$getTranslationKey();
    }

    public Translation prismarine$getTranslation() {
        if (this.api$translation == null) {
            this.api$translation = new SpongeTranslation("tile.prismarine." + shadow$getTranslationKey() + ".name");
        }
        return this.api$translation;
    }

}
