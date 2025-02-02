package com.glodblock.github.util;

import net.minecraftforge.fml.common.Loader;

public final class ModAndClassUtil {

    public static boolean AUTO_P = false;
    public static boolean GT = false;
    public static boolean NEE = false;
    public static boolean DY = false;
    public static boolean OC = false;
    public static boolean GAS = false;

    public static void init() {
        if (Loader.isModLoaded("packagedauto")) {
            AUTO_P = true;
        }

        if (Loader.isModLoaded("gregtech")) {
            GT = true;
        }

        if (Loader.isModLoaded("neenergistics")) {
            NEE = true;
        }

        if (Loader.isModLoaded("dynamistics")) {
            DY = true;
        }

        if (Loader.isModLoaded("opencomputers")) {
            OC = true;
        }

        if (Loader.isModLoaded("mekeng")) {
            GAS = true;
        }
    }

}
