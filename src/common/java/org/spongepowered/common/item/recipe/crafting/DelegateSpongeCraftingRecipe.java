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
package org.spongepowered.common.item.recipe.crafting;

import com.google.common.base.Preconditions;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.ShapedCraftingRecipe;
import org.spongepowered.api.item.recipe.crafting.ShapelessCraftingRecipe;
import org.spongepowered.api.world.World;
import org.spongepowered.common.item.inventory.util.ItemStackUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.spongepowered.common.item.inventory.util.InventoryUtil.toSpongeInventory;

public class DelegateSpongeCraftingRecipe implements IRecipe {

    private final CraftingRecipe recipe;

    public DelegateSpongeCraftingRecipe(CraftingRecipe recipe) {
        Preconditions.checkNotNull(recipe, "recipe");

        this.recipe = recipe;
    }

    public CraftingRecipe getDelegate() {
        return this.recipe;
    }

    @Override
    public IRecipe setRegistryName(ResourceLocation name) {
        throw new UnsupportedOperationException("Cannot set a different name for a DelegatedSpongeCraftingRecipe.");
    }

    @Nullable
    @Override
    public ResourceLocation getRegistryName() {
        return new ResourceLocation(this.recipe.getId());
    }

    @Override
    public Class<IRecipe> getRegistryType() {
        return IRecipe.class;
    }

    @Override
    public boolean isDynamic() {
        return true; // For RecipeBook
    }

    @Override
    public boolean canFit(int width, int height) {
        if (this.recipe instanceof ShapedCraftingRecipe) {
            ShapedCraftingRecipe recipe = (ShapedCraftingRecipe) this;
            return recipe.getWidth() >= width && recipe.getHeight() >= height;
        }
        if (this.recipe instanceof ShapelessCraftingRecipe) {
            return width <= 2 && height <= 2;
        }
        return true; // TODO: investigate side-effects
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        if (this.recipe instanceof ShapedCraftingRecipe) {
            ShapedCraftingRecipe recipe = (ShapedCraftingRecipe) this;
            NonNullList<Ingredient> ingredients = NonNullList.create();
            for (int x = 0; x < recipe.getWidth(); x++) {
                for (int y = 0; y < recipe.getHeight(); y++) {
                    ingredients.add(IngredientUtil.toNative(recipe.getIngredient(x, y)));
                }
            }
            return ingredients;
        }
        if (this.recipe instanceof ShapelessCraftingRecipe) {
            return ((ShapelessCraftingRecipe) this.recipe).getIngredientPredicates()
                    .stream()
                    .map(IngredientUtil::toNative)
                    .collect(Collectors.toCollection(NonNullList::create));
        }
        return NonNullList.create();
    }

    @Override
    public String getGroup() {
        return this.recipe.getGroup().orElse("");
    }

    @Override
    public boolean matches(InventoryCrafting inv, net.minecraft.world.World worldIn) {
        return matches(this.recipe::isValid, inv, worldIn);
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return getCraftingResult(this.recipe::getResult, inv);
    }

    @Override
    public ItemStack getRecipeOutput() {
        return getRecipeOutput(this.recipe::getExemplaryResult);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return getRemainingItems(this.recipe::getRemainingItems, inv);
    }

    public static boolean matches(BiFunction<CraftingGridInventory, World, Boolean> isValid, InventoryCrafting inv, net.minecraft.world.World worldIn) {
        return isValid.apply(toSpongeInventory(inv), (World) worldIn);
    }

    public static ItemStack getCraftingResult(Function<CraftingGridInventory, ItemStackSnapshot> getResult, InventoryCrafting inv) {
        ItemStackSnapshot result = getResult.apply(toSpongeInventory(inv));

        Preconditions.checkNotNull(result, "The Sponge implementation returned a `null` result.");

        return ItemStackUtil.fromSnapshotToNative(result);
    }

    public static ItemStack getRecipeOutput(Supplier<ItemStackSnapshot> getExemplaryResult) {
        return ItemStackUtil.fromSnapshotToNative(getExemplaryResult.get());
    }

    public static NonNullList<ItemStack> getRemainingItems(Function<CraftingGridInventory, List<ItemStackSnapshot>> getRemainingItems, InventoryCrafting inv) {
        List<ItemStackSnapshot> spongeResult = getRemainingItems.apply(toSpongeInventory(inv));

        if (spongeResult.size() != inv.getSizeInventory()) {
            throw new IllegalStateException("The number of ItemStackSnapshots returned by getRemainingItems must be equal to the size of the GridInventory.");
        }

        NonNullList<ItemStack> result = NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);

        for(int i = 0; i < spongeResult.size(); i++) {
            ItemStack item = ItemStackUtil.fromSnapshotToNative(spongeResult.get(i));

            result.set(i, item);
        }

        return result;
    }
}
