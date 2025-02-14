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
package org.spongepowered.common.mixin.api.mcp.item.crafting;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.asm.mixin.*;

@Mixin(ShapedRecipes.class)
@Implements(value = @Interface(iface = ShapedCraftingRecipe.class, prefix = "recipe$"))
public abstract class ShapedRecipesMixin_API implements IRecipe {

    @Shadow @Final public int recipeWidth;
    @Shadow @Final public int recipeHeight;
    @Shadow @Final public NonNullList<Ingredient> recipeItems;

    public org.spongepowered.api.item.recipe.crafting.Ingredient recipe$getIngredient(final int x, final int y) {
        if (x < 0 || x >= this.recipeWidth || y < 0 || y >= this.recipeHeight) {
            throw new IndexOutOfBoundsException("Invalid ingredient predicate location");
        }

        final int recipeItemIndex = x + y * this.recipeWidth;
        return ((org.spongepowered.api.item.recipe.crafting.Ingredient)(Object) this.recipeItems.get(recipeItemIndex));
    }

    public int recipe$getWidth() {
        return this.recipeWidth;
    }

    public int recipe$getHeight() {
        return this.recipeHeight;
    }

}
