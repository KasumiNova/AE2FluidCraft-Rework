package com.glodblock.github.integration.jei;

import com.glodblock.github.common.item.fake.FakeFluids;
import com.glodblock.github.integration.gregtech.GregUtil;
import com.glodblock.github.integration.mek.FakeGases;
import com.glodblock.github.util.Ae2Reflect;
import com.glodblock.github.util.ModAndClassUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mekanism.api.gas.GasStack;
import mekanism.client.jei.MekanismJEI;
import mezz.jei.api.gui.IGuiIngredient;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.gui.recipes.RecipeLayout;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RecipeTransferBuilder {

    private static final int MAX_ITEMS = 16;
    private static Field fRecipeLayout_recipeWrapper;

    private final Int2ObjectArrayMap<ItemStack[]> in;
    private final List<ItemStack> out;
    private final IRecipeLayout recipe;
    private List<ItemStack[]> itemsIn;
    private List<FluidStack> fluidIn;
    private List<Object> gasIn;
    private List<ItemStack> itemOut;
    private List<FluidStack> fluidOut;
    private List<Object> gasOut;
    private boolean noNull = true;
    private boolean fluidFirst = false;

    static {
        try {
            fRecipeLayout_recipeWrapper = Ae2Reflect.reflectField(RecipeLayout.class, "recipeWrapper");
        } catch (NoSuchFieldException ignore) {
            // NO-OP
        }
    }

    public RecipeTransferBuilder(IRecipeLayout recipe) {
        this.in = new Int2ObjectArrayMap<>();
        this.out = new ArrayList<>();
        this.recipe = recipe;
        this.itemsIn = new ArrayList<>();
        this.itemOut = new ArrayList<>();
        this.fluidIn = new ArrayList<>();
        this.fluidOut = new ArrayList<>();
        this.gasIn = new ArrayList<>();
        this.gasOut = new ArrayList<>();
        this.split();
    }

    private void split() {
        for (int index : this.recipe.getItemStacks().getGuiIngredients().keySet()) {
            IGuiIngredient<ItemStack> ing = this.recipe.getItemStacks().getGuiIngredients().get(index);
            if (ing.isInput()) {
                if (ModAndClassUtil.GT) {
                    if (GregUtil.isNotConsume(this.getWrapper(this.recipe), index)) {
                        continue;
                    }
                }
                List<ItemStack> holder;
                if (ing.getAllIngredients().size() < MAX_ITEMS - 1) {
                    holder = ing.getAllIngredients();
                } else {
                    holder = ing.getAllIngredients().subList(0, MAX_ITEMS - 1);
                }
                // Put displayed item at first check
                if (ing.getDisplayedIngredient() != null) {
                    holder.add(0, ing.getDisplayedIngredient());
                }
                this.itemsIn.add(holder.toArray(new ItemStack[0]));
            } else {
                this.itemOut.add(ing.getDisplayedIngredient());
            }
        }
        for (IGuiIngredient<FluidStack> ing : this.recipe.getFluidStacks().getGuiIngredients().values()) {
            if (ing.isInput()) {
                this.fluidIn.add(ing.getDisplayedIngredient());
            } else {
                this.fluidOut.add(ing.getDisplayedIngredient());
            }
        }
        if (ModAndClassUtil.GAS) {
            for (IGuiIngredient<GasStack> ing : this.recipe.getIngredientsGroup(MekanismJEI.TYPE_GAS).getGuiIngredients().values()) {
                if (ing.isInput()) {
                    this.gasIn.add(ing.getDisplayedIngredient());
                } else {
                    this.gasOut.add(ing.getDisplayedIngredient());
                }
            }
        }
    }

    private void setItemIn(int offset) {
        int bound = this.itemsIn.size() + offset;
        for (int index = offset; index < bound; index ++) {
            int i = index - offset;
            if (this.itemsIn.get(i) != null && this.itemsIn.get(i).length > 0) {
                this.in.put(index, this.itemsIn.get(i));
            }
        }
    }

    private void setFluidIn(int offset) {
        int bound = this.fluidIn.size() + this.gasIn.size() + offset;
        for (int index = offset; index < bound; index ++) {
            int i = index - offset;
            if (i < this.fluidIn.size()) {
                if (this.fluidIn.get(i) != null) {
                    this.in.put(index, new ItemStack[] {FakeFluids.packFluid2Packet(this.fluidIn.get(i))});
                }
            } else {
                i -= this.fluidIn.size();
                if (this.gasIn.get(i) != null) {
                    this.in.put(index, new ItemStack[] {FakeGases.packGas2Packet((GasStack) this.gasIn.get(i))});
                }
            }
        }
    }

    private void setOutputs() {
        for (int index = 0; index < this.itemOut.size() + this.fluidOut.size() + this.gasOut.size(); index ++) {
            if (index < this.itemOut.size()) {
                this.out.add(this.itemOut.get(index));
            } else if (index - this.itemOut.size() < this.fluidOut.size()) {
                this.out.add(FakeFluids.packFluid2Packet(this.fluidOut.get(index - this.itemOut.size())));
            } else if (index - this.itemOut.size() - this.fluidOut.size() < this.gasOut.size()) {
                this.out.add(FakeGases.packGas2Packet((GasStack) this.gasOut.get(index - this.itemOut.size() - this.fluidOut.size())));
            }
        }
    }

    public RecipeTransferBuilder clearEmptySlot(boolean val) {
        this.noNull = val;
        return this;
    }

    public RecipeTransferBuilder putFluidFirst(boolean val) {
        this.fluidFirst = val;
        return this;
    }

    public RecipeTransferBuilder build() {
        if (this.noNull) {
            this.itemsIn = this.itemsIn.stream().filter(o -> o != null && o.length > 0).collect(Collectors.toList());
            this.itemOut = this.itemOut.stream().filter(Objects::nonNull).collect(Collectors.toList());
            this.fluidIn = this.fluidIn.stream().filter(Objects::nonNull).collect(Collectors.toList());
            this.fluidOut = this.fluidOut.stream().filter(Objects::nonNull).collect(Collectors.toList());
            this.gasIn = this.gasIn.stream().filter(Objects::nonNull).collect(Collectors.toList());
            this.gasOut = this.gasOut.stream().filter(Objects::nonNull).collect(Collectors.toList());
        }
        if (this.fluidFirst) {
            this.setFluidIn(0);
            this.setItemIn(this.fluidIn.size() + this.gasIn.size());
        } else {
            this.setItemIn(0);
            this.setFluidIn(this.itemsIn.size());
        }
        this.setOutputs();
        return this;
    }

    public List<ItemStack> getOutput() {
        return this.out;
    }

    public Int2ObjectMap<ItemStack[]> getInput() {
        return this.in;
    }

    private IRecipeWrapper getWrapper(IRecipeLayout recipe) {
        if (fRecipeLayout_recipeWrapper != null) {
            return Ae2Reflect.readField(recipe, fRecipeLayout_recipeWrapper);
        }
        return null;
    }

}
