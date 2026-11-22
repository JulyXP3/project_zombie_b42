package modcore;

import org.junit.jupiter.api.Test;
import se.krka.kahlua.vm.KahluaTable;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ItemScanFloorLabelTest {
    @Test void absoluteFloorsStayDistinctInChineseEnglishAndRussianAfterClimbing() throws Exception {
        String[][] wanted={{"3楼","4楼","5楼","地下1层","地下2层"},
                {"Floor 3","Floor 4","Floor 5","Basement 1","Basement 2"},
                {"Этаж 3","Этаж 4","Этаж 5","Подвал 1","Подвал 2"}};
        String[] languages={"CN","EN","RU"};
        var lua=ItemSearchTestRuntime.create();var translations=(KahluaTable)lua.environment.rawget("translations");
        for(int i=0;i<languages.length;i++){
            translations.rawset("UI_RadarPanel_FloorNumber",text(languages[i],"UI_RadarPanel_FloorNumber"));
            translations.rawset("UI_RadarPanel_BasementNumber",text(languages[i],"UI_RadarPanel_BasementNumber"));
            Object[] actual=lua.execute("""
                    if type(EtherItemSearch.refreshFloorLabels)=="function" then EtherItemSearch.refreshFloorLabels() end
                    if type(EtherItemSearch.floorLabel)~="function" then return nil,nil,nil,nil,nil end
                    player.z=0
                    local a,b,c,d,e=EtherItemSearch.floorLabel(2),EtherItemSearch.floorLabel(3),EtherItemSearch.floorLabel(4),EtherItemSearch.floorLabel(-1),EtherItemSearch.floorLabel(-2)
                    player.z=4
                    return EtherItemSearch.floorLabel(2),EtherItemSearch.floorLabel(3),EtherItemSearch.floorLabel(4),EtherItemSearch.floorLabel(-1),EtherItemSearch.floorLabel(-2)
                    """);
            assertArrayEquals(wanted[i],actual,languages[i]);
        }
    }
    @Test void repeatedFloorFormattingUsesNoPerMarkerTranslationGetter() throws Exception {
        var lua=ItemSearchTestRuntime.create();
        ((KahluaTable)lua.environment.rawget("translations")).rawset("UI_RadarPanel_FloorNumber","Floor {n}");
        ((KahluaTable)lua.environment.rawget("translations")).rawset("UI_RadarPanel_BasementNumber","Basement {n}");
        Object[] actual=lua.execute("""
                if type(EtherItemSearch.refreshFloorLabels)~="function" or type(EtherItemSearch.floorLabel)~="function" then return false,-1 end
                EtherItemSearch.refreshFloorLabels()
                local original=getTranslate;local calls=0
                getTranslate=function(...) calls=calls+1;return original(...) end
                for i=1,100 do EtherItemSearch.floorLabel(4);EtherItemSearch.floorLabel(-2) end
                return true,calls
                """);
        assertArrayEquals(new Object[]{true,0.0},actual);
    }
    private static String text(String language,String key) throws Exception {
        try(var in=ItemScanFloorLabelTest.class.getClassLoader().getResourceAsStream("modcore/translations/"+language+".txt")){
            assertNotNull(in);String source=new String(in.readAllBytes(),StandardCharsets.UTF_8);
            for(String line:source.lines().toList()){
                String clean=line.stripLeading();
                if(clean.startsWith(key)&&clean.substring(key.length()).stripLeading().startsWith("=")){
                    int first=line.indexOf('"'),last=line.lastIndexOf('"');
                    if(first>=0&&last>first)return line.substring(first+1,last);
                }
            }
            return key;
        }
    }
}
