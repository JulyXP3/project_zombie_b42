/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  se.krka.kahlua.converter.KahluaConverterManager
 *  se.krka.kahlua.integration.expose.LuaJavaClassExposer
 *  se.krka.kahlua.j2se.J2SEPlatform
 *  se.krka.kahlua.vm.KahluaTable
 *  se.krka.kahlua.vm.Platform
 */
package EtherHack.utils;

import EtherHack.Ether.EtherAPI;
import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.integration.expose.LuaJavaClassExposer;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.Platform;

public class Exposer
extends LuaJavaClassExposer {
    public Exposer(KahluaConverterManager var1, J2SEPlatform var2, KahluaTable var3) {
        super(var1, (Platform)var2, var3);
    }

    public void exposeAPI(EtherAPI.SafeEtherLuaMethods var1) {
        this.exposeGlobalFunctions(var1);
    }
}
