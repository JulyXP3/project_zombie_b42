require "ISUI/ISPanel"

--*********************************************************
--* Глобальные установки UI
--*********************************************************
EtherInfoPanel = ISPanel:derive("Dei0InfoPanel"); -- Наследование от ISPanel

--*********************************************************
--* Метод рисования текста по середние
--*********************************************************
function EtherInfoPanel:drawTextCentered(text, y, r, g, b, a, font)
    local x = self.width / 2 - (getTextManager():MeasureStringX(font, text) / 2)
    self:drawText(text, x, y, r, g, b, a, font)
end

--*********************************************************
--* Отрисовка текста
--*********************************************************
function EtherInfoPanel:render()
    local th = EtherTheme;
    local fhS = th.fontHgtSmall;
    local fhM = th.fontHgtMedium;
    local y = fhM + 10
    local mTitle = fhM + 10;
    local mText = fhS + 8;

    self:drawTextCentered(getTranslate("UI_InformationPanel_General_Title"), y, th.blood.r, th.blood.g, th.blood.b, 1, UIFont.Medium)

    y = y + mTitle

    local generalTexts = {
        "UI_InformationPanel_General_Text1",
        "UI_InformationPanel_General_Text2",
        "UI_InformationPanel_General_Text3",
        "UI_InformationPanel_General_Text4",
        "UI_InformationPanel_General_Text5",
    }
    for i, textKey in ipairs(generalTexts) do
        self:drawTextCentered(getTranslate(textKey), y, th.text.r, th.text.g, th.text.b, 1, UIFont.Small)
        y = y + mText
    end

    y = y + mText

    self:drawTextCentered(getTranslate("UI_InformationPanel_Disclaimer_Title"), y, th.blood.r, th.blood.g, th.blood.b, 1, UIFont.Medium)

    y = y + mTitle

    local disclaimerTexts = {
        "UI_InformationPanel_Disclaimer_Text1",
        "UI_InformationPanel_Disclaimer_Text2",
        "UI_InformationPanel_Disclaimer_Text3",
        "UI_InformationPanel_Disclaimer_Text4",
        "UI_InformationPanel_Disclaimer_Text5",
    }
    for i, textKey in ipairs(disclaimerTexts) do
        self:drawTextCentered(getTranslate(textKey), y, th.textDim.r, th.textDim.g, th.textDim.b, 1, UIFont.Small)
        y = y + mText
    end

    y = y + mText

    self:drawTextCentered(getTranslate("UI_InformationPanel_AntiCheatStatus_Title"), y, th.blood.r, th.blood.g, th.blood.b, 1, UIFont.Medium)

    y = y + mTitle

    local antiCheatStatusTexts = {
        "UI_InformationPanel_AntiCheatStatus_Text1",
        "UI_InformationPanel_AntiCheatStatus_Text2",
        "UI_InformationPanel_AntiCheatStatus_BikiniTools",
        "UI_InformationPanel_AntiCheatStatus_CustomLogger"
    }

    local customLogger = PARP ~= nil or LogExtenderClient ~= nil or LogExtenderServer ~= nil or AVCS ~= nil;
    local bikinitools = BTSE ~= nil or PARP ~= nil or Bikinitools ~= nil;

    local antiCheatStatus = {
        getAntiCheat12Status(),
        getAntiCheat8Status(),
        bikinitools,
        customLogger
    }

    for i, textKey in ipairs(antiCheatStatusTexts) do
        local statusEnabled = antiCheatStatus[i];
        if self.localPlayer == nil then statusEnabled = false end

        local statusText = statusEnabled
                        and getTranslate("UI_InformationPanel_AntiCheatStatus_Enable")
                        or getTranslate("UI_InformationPanel_AntiCheatStatus_Disable")
        local baseText = getTranslate(textKey)

        if statusEnabled then
            self:drawTextCentered(baseText, y, th.text.r, th.text.g, th.text.b, 1, UIFont.Small)
            self:drawTextCentered(statusText, y + fhS, EtherMain.accentColor.r, EtherMain.accentColor.g, EtherMain.accentColor.b, 1, UIFont.Small)
        else
            self:drawTextCentered(baseText, y, th.text.r, th.text.g, th.text.b, 0.55, UIFont.Small)
            self:drawTextCentered(statusText, y + fhS, 0.8, 0.2, 0.2, 0.8, UIFont.Small)
        end
        y = y + fhS * 2 + 4
    end


    y = y + mText

    self:drawTextCentered(getTranslate("UI_InformationPanel_Contacts_Title"), y, th.blood.r, th.blood.g, th.blood.b, 1, UIFont.Medium)

    y = y + mTitle

    local contactTexts = {
        "GitHub: None",
        "Discord: None",
        "Email: None",
        getTranslate("UI_InformationPanel_Contacts_Donation") .. "Contact me for details.",
    }
    for i, text in ipairs(contactTexts) do
        self:drawTextCentered(text, y, th.textDim.r, th.textDim.g, th.textDim.b, 1, UIFont.Small)
        y = y + mText
    end
end

--*********************************************************
--* Создание нового экземпляра меню
--*********************************************************
function EtherInfoPanel:new(posX, posY, width, height)
    local menuTableData = {};

    menuTableData = ISPanel:new(posX, posY, width, height);
    setmetatable(menuTableData, self);
    menuTableData.background = true;
	menuTableData.backgroundColor = {r=0.0, g=0.0, b=0.0, a=0.0};
	menuTableData.borderColor = {r=0.0, g=0.0, b=0.0, a=0.0};
    menuTableData.moveWithMouse = true;
    menuTableData.localPlayer = getPlayer();
    self.__index = self;

    return menuTableData;
end