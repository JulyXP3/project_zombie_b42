require "OptionScreens/CharacterCreationMain"

--*********************************************************
--* EtherCharacterCreation: 建号界面服装门控覆盖
--*
--* 「解锁全部服装」(创建角色选项卡): 勾选后强制建号界面显示
--* 全量服装选择器 (原版由沙盒选项 AllClothesUnlocked 或调试选项
--* Character.Create.AllOutfits 门控, 默认关闭; 多人下沙盒由服务器
--* 控制, 本开关提供客户端侧解锁)。未勾选时走原版逻辑。
--* checkAllClothingOptions() 在建号界面 update() 每帧调用,
--* 门控变化即时重建服装 UI (initClothing/initClothingDebug)。
--* CoopCharacterCreationMain 继承本类方法, 多人建号同样生效。
--*********************************************************

local vanillaShouldShowAllOutfits = CharacterCreationMain.shouldShowAllOutfits;

function CharacterCreationMain:shouldShowAllOutfits()
	if isCharCreateAllClothes and isCharCreateAllClothes() then
		return true;
	end
	return vanillaShouldShowAllOutfits(self);
end
