package com.mooar.market.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NFT 属性字典：按合约定义 trait 槽位与枚举值编码
 *
 * 编码规则：枚举值在 enumOf 中的顺序即编码（1 起始），新增值只能追加，已占用的编码不可调整，
 * 否则历史 nft 槽位数据语义会错位。
 */
public final class TraitDict {

    private static final Map<String, Map<String, TraitDef>> DICT = new HashMap<>();

    static {
        Map<String, TraitDef> defs0 = new LinkedHashMap<>();
        defs0.put("State", TraitDef.enumOf("State", "v1",
                "Active"
        ));
        DICT.put("0x74796fd8c0ef5c17340f38f5ff672580d3e57f2d", defs0);

        Map<String, TraitDef> defs1 = new LinkedHashMap<>();
        defs1.put("Background", TraitDef.enumOf("Background", "v1",
                "80s Horizontal Blue (Common)", "80s Horizontal Cyan (Common)", "80s Horizontal Green (Common)", "80s Horizontal Grey (Common)",
                "80s Horizontal Red (Common)", "80s Horizontal Yellow (Common)", "ADL Purple (Common)", "Error Blue (Legendary)",
                "Error Green (Legendary)", "Error Grey (Legendary)", "Error Orange (Legendary)", "Error Red (Legendary)",
                "Error Yellow (Legendary)", "Game Blue (Common)", "Game Green (Common)", "Game Grey (Common)",
                "Game Purple (Common)", "Game Yellow (Common)", "Halloween Purple (Common)", "Pixel Blue (Rare)",
                "Pixel Green (Rare)", "Pixel Grey (Rare)", "Pixel Red (Rare)", "Pixel Yellow (Rare)"
        ));
        defs1.put("Body", TraitDef.enumOf("Body", "v2",
                "Marty Armour Blue (Rare)", "Marty Armour Green (Rare)", "Marty Armour Purple (Rare)", "Marty Armour Yellow (Rare)",
                "Marty Flameset Blue (Legendary)", "Marty Flameset Purple (Legendary)", "Marty Flameset Red (Legendary)", "Marty Flameset Yellow (Legendary)",
                "Marty STEPN Blue (Common)", "Marty STEPN Green (Common)", "Marty STEPN Grey (Common)", "Marty STEPN Red (Common)",
                "Marty VR Pink (Common)", "Marty VR Purple (Common)", "Marty VR Red (Common)", "Marty VR Yellow (Common)",
                "Miffy Armour Blue (Rare)", "Miffy Armour Green (Rare)", "Miffy Armour Purple (Rare)", "Miffy Armour Yellow (Rare)",
                "Miffy Flameset Blue (Legendary)", "Miffy Flameset Purple (Legendary)", "Miffy Flameset Red (Legendary)", "Miffy Flameset Yellow (Legendary)",
                "Miffy STEPN Blue (Common)", "Miffy STEPN Green (Common)", "Miffy STEPN Purple (Common)", "Miffy STEPN Red (Common)",
                "Miffy VR Blue (Common)", "Miffy VR Green (Common)", "Miffy VR Pink (Common)", "Miffy VR Red (Common)",
                "Mog Armour Blue (Rare)", "Mog Armour Green (Rare)", "Mog Armour Purple (Rare)", "Mog Armour Yellow (Rare)",
                "Mog Flameset Blue (Legendary)", "Mog Flameset Purple (Legendary)", "Mog Flameset Red (Legendary)", "Mog Flameset Yellow (Legendary)",
                "Mog STEPN Blue (Common)", "Mog STEPN Green (Common)", "Mog STEPN Orange (Common)", "Mog STEPN Pink (Common)",
                "Mog VR Blue (Common)", "Mog VR Green (Common)", "Mog VR Orange (Common)", "Mog VR Pink (Common)",
                "Mooarty ADL Purple (Common)", "Mooarty Armour Blue (Rare)", "Mooarty Armour Green (Rare)", "Mooarty Armour Purple (Rare)",
                "Mooarty Armour Red (Rare)", "Mooarty Flameset Blue (Legendary)", "Mooarty Flameset Purple (Legendary)", "Mooarty Flameset Red (Legendary)",
                "Mooarty Flameset Yellow (Legendary)", "Mooarty Mid-Autumn Orange (Common)", "Mooarty STEPN Blue (Common)", "Mooarty STEPN Brown (Common)",
                "Mooarty STEPN Grey (Common)", "Mooarty STEPN Purple (Common)", "Mooarty VR Blue (Common)", "Mooarty VR Green (Common)",
                "Mooarty VR Purple (Common)", "Mooarty VR Yellow (Common)", "Mooarty Vampire Black (Common)", "Mooarty Zombie Green (Common)"
        ));
        defs1.put("Character", TraitDef.enumOf("Character", "v3",
                "Marty", "Miffy", "Mog", "Mooarty"
        ));
        defs1.put("Headgear", TraitDef.enumOf("Headgear", "v4",
                "Marty Armour Blue (Rare)", "Marty Armour Green (Rare)", "Marty Armour Purple (Rare)", "Marty Armour Yellow (Rare)",
                "Marty Flameset Blue (Legendary)", "Marty Flameset Green (Legendary)", "Marty Flameset Purple (Legendary)", "Marty Flameset Yellow (Legendary)",
                "Marty STEPN Blue (Common)", "Marty STEPN Green (Common)", "Marty STEPN Grey (Common)", "Marty STEPN Red (Common)",
                "Marty VR Pink (Common)", "Marty VR Purple (Common)", "Marty VR Red (Common)", "Marty VR Yellow (Common)",
                "Miffy Armour Blue (Rare)", "Miffy Armour Green (Rare)", "Miffy Armour Purple (Rare)", "Miffy Armour Yellow (Rare)",
                "Miffy Flameset Blue (Legendary)", "Miffy Flameset Orange (Legendary)", "Miffy Flameset Purple (Legendary)", "Miffy Flameset Red (Legendary)",
                "Miffy STEPN Blue (Common)", "Miffy STEPN Green (Common)", "Miffy STEPN Purple (Common)", "Miffy STEPN Red (Common)",
                "Miffy VR Blue (Common)", "Miffy VR Green (Common)", "Miffy VR Pink (Common)", "Miffy VR Red (Common)",
                "Mog Armour Blue (Rare)", "Mog Armour Green (Rare)", "Mog Armour Purple (Rare)", "Mog Armour Yellow (Rare)",
                "Mog Flameset Blue (Legendary)", "Mog Flameset Orange (Legendary)", "Mog Flameset Purple (Legendary)", "Mog Flameset Red (Legendary)",
                "Mog STEPN Blue (Common)", "Mog STEPN Green (Common)", "Mog STEPN Pink (Common)", "Mog STEPN Red (Common)",
                "Mog VR Blue (Common)", "Mog VR Green (Common)", "Mog VR Orange (Common)", "Mog VR Pink (Common)",
                "Mooarty ADL Purple (Common)", "Mooarty Armour Blue (Rare)", "Mooarty Armour Green (Rare)", "Mooarty Armour Purple (Rare)",
                "Mooarty Armour Yellow (Rare)", "Mooarty Flameset Blue (Legendary)", "Mooarty Flameset Green (Legendary)", "Mooarty Flameset Orange (Legendary)",
                "Mooarty Flameset Purple (Legendary)", "Mooarty Flameset Red (Legendary)", "Mooarty Mid-Autumn Orange (Common)", "Mooarty STEPN Blue (Common)",
                "Mooarty STEPN Grey (Common)", "Mooarty STEPN Purple (Common)", "Mooarty STEPN Yellow (Common)", "Mooarty VR Green (Common)",
                "Mooarty VR Grey (Common)", "Mooarty VR Purple (Common)", "Mooarty VR Red (Common)", "Mooarty Vampire Black (Common)",
                "Mooarty Zombie Green (Common)"
        ));
        defs1.put("Rarity", TraitDef.enumOf("Rarity", "v5",
                "Common", "Legendary", "Rare"
        ));
        defs1.put("Sneaker", TraitDef.enumOf("Sneaker", "v6",
                "Marty Armour Blue (Rare)", "Marty Armour Green (Rare)", "Marty Armour Purple (Rare)", "Marty Armour Yellow (Rare)",
                "Marty Flameset Blue (Legendary)", "Marty Flameset Orange (Legendary)", "Marty Flameset Purple (Legendary)", "Marty Flameset Red (Legendary)",
                "Marty STEPN Blue (Common)", "Marty STEPN Green (Common)", "Marty STEPN Red (Common)", "Marty STEPN Yellow (Common)",
                "Marty VR Pink (Common)", "Marty VR Purple (Common)", "Marty VR Red (Common)", "Marty VR Yellow (Common)",
                "Miffy Armour Blue (Rare)", "Miffy Armour Green (Rare)", "Miffy Armour Purple (Rare)", "Miffy Armour Yellow (Rare)",
                "Miffy Flameset Blue (Legendary)", "Miffy Flameset Orange (Legendary)", "Miffy Flameset Purple (Legendary)", "Miffy Flameset Red (Legendary)",
                "Miffy STEPN Blue (Common)", "Miffy STEPN Green (Common)", "Miffy STEPN Orange (Common)", "Miffy STEPN Red (Common)",
                "Miffy VR Blue (Common)", "Miffy VR Green (Common)", "Miffy VR Pink (Common)", "Miffy VR Red (Common)",
                "Mog Armour Blue (Rare)", "Mog Armour Green (Rare)", "Mog Armour Purple (Rare)", "Mog Armour Yellow (Rare)",
                "Mog Flameset Blue (Legendary)", "Mog Flameset Purple (Legendary)", "Mog Flameset Red (Legendary)", "Mog Flameset Yellow (Legendary)",
                "Mog STEPN Blue (Common)", "Mog STEPN Green (Common)", "Mog STEPN Pink (Common)", "Mog STEPN Red (Common)",
                "Mog VR Blue (Common)", "Mog VR Green (Common)", "Mog VR Orange (Common)", "Mog VR Pink (Common)",
                "Mooarty ADL Purple (Common)", "Mooarty Armour Blue (Rare)", "Mooarty Armour Purple (Rare)", "Mooarty Armour Red (Rare)",
                "Mooarty Armour Yellow (Rare)", "Mooarty Flamset Blue (Legendary)", "Mooarty Flamset Orange (Legendary)", "Mooarty Flamset Purple (Legendary)",
                "Mooarty Flamset Red (Legendary)", "Mooarty Mid-Autumn Orange (Common)", "Mooarty STEPN Blue (Common)", "Mooarty STEPN Green (Common)",
                "Mooarty STEPN Orange (Common)", "Mooarty STEPN Red (Common)", "Mooarty VR Blue (Common)", "Mooarty VR Green (Common)",
                "Mooarty VR Purple (Common)", "Mooarty VR Yellow (Common)", "Mooarty Vampire Black (Common)", "Mooarty Zombie Green (Common)"
        ));
        defs1.put("Sticker", TraitDef.enumOf("Sticker", "v7",
                "Marty Armour (Rare)", "Miffy Armour (Rare)", "Mog Armour (Rare)", "Mooarty Armour (Rare)"
        ));
        DICT.put("0x462dd5c6a786acb47c60ad419feab11054b0b249", defs1);

        Map<String, TraitDef> defs2 = new LinkedHashMap<>();
        defs2.put("Rarity", TraitDef.enumOf("Rarity", "v1",
                "Common", "Legendary", "Rare"
        ));
        defs2.put("Type", TraitDef.enumOf("Type", "v2",
                "Genesis", "Invitation Mint"
        ));
        defs2.put("Level", TraitDef.number("Level", "level", 0, 100));
        defs2.put("Character", TraitDef.enumOf("Character", "v3",
                "Iggy", "Marty", "Miffy", "Mog",
                "Mooarty", "Spesh"
        ));
        defs2.put("Device", TraitDef.enumOf("Device", "v4",
                "Angry Dino (Common)", "Blast (Common)", "Box Green (Common)", "Box Grey (Common)",
                "Box Purple (Common)", "Box Red (Common)", "Box Teal (Common)", "Box Yellow (Common)",
                "Controller Green (Legendary)", "Controller Grey (Legendary)", "Controller Purple (Legendary)", "Controller Red (Legendary)",
                "Controller Yellow (Legendary)", "Coolman Iggy White (Rare)", "Coolman Spesh Green (Rare)", "Disk Grey (Common)",
                "Disk Purple (Common)", "Disk Red (Common)", "Disk Teal (Common)", "Disk Yellow (Common)",
                "Famous Foxes (Common)", "Halloween Red (Common)", "Halloween Red (Legendary)", "Halloween Red (Rare)",
                "LOK (Common)", "Mid-Autumn Orange (Common)", "Plato (Common)", "Round Blue (Rare)",
                "Round Green (Rare)", "Round Grey (Rare)", "Round Purple (Rare)", "Round Teal (Rare)",
                "Top Goal (Common)"
        ));
        defs2.put("Screen", TraitDef.enumOf("Screen", "v5",
                "80s Horizontal Blue (Common)", "80s Horizontal Cyan (Common)", "80s Horizontal Green (Common)", "80s Horizontal Grey (Common)",
                "80s Horizontal Red (Common)", "80s Horizontal Yellow (Common)", "ADL Purple (Common)", "Coolman Iggy Yellow (Rare)",
                "Coolman Spesh Yellow (Rare)", "Error Blue (Legendary)", "Error Green (Legendary)", "Error Grey (Legendary)",
                "Error Orange (Legendary)", "Error Red (Legendary)", "Error Yellow (Legendary)", "Game Blue (Common)",
                "Game Green (Common)", "Game Grey (Common)", "Game Purple (Common)", "Game Yellow (Common)",
                "Halloween Purple (Common)", "Pixel Blue (Rare)", "Pixel Green (Rare)", "Pixel Grey (Rare)",
                "Pixel Red (Rare)", "Pixel Yellow (Rare)"
        ));
        defs2.put("Background", TraitDef.enumOf("Background", "v6",
                "90s kid Green", "90s kid Orange", "90s kid Purple", "90s kid blue",
                "ADL", "BANX", "Coolman Coffedan", "Cyberpunk Green",
                "Cyberpunk Orange", "Cyberpunk Purple", "Cyberpunk Red", "DEFILAND",
                "HONEYLAND", "Halloween", "MATR1X", "Mid-Autumn",
                "PLUTO", "Psychedelic Green", "Psychedelic Purple", "Psychedelic Red",
                "Psychedelic blue", "RAGE EFFECT", "SPACE MAVERICKS", "Sticker V1",
                "Sticker V2", "Sticker V3", "Sticker V4"
        ));
        defs2.put("Body", TraitDef.enumOf("Body", "v7",
                "Iggy Coolman Iggy Grey (Rare)", "Marty Armour Blue (Rare)", "Marty Armour Green (Rare)", "Marty Armour Purple (Rare)",
                "Marty Armour Yellow (Rare)", "Marty Flameset Blue (Legendary)", "Marty Flameset Purple (Legendary)", "Marty Flameset Red (Legendary)",
                "Marty Flameset Yellow (Legendary)", "Marty STEPN Blue (Common)", "Marty STEPN Green (Common)", "Marty STEPN Grey (Common)",
                "Marty STEPN Red (Common)", "Marty VR Pink (Common)", "Marty VR Purple (Common)", "Marty VR Red (Common)",
                "Marty VR Yellow (Common)", "Miffy Armour Blue (Rare)", "Miffy Armour Green (Rare)", "Miffy Armour Purple (Rare)",
                "Miffy Armour Yellow (Rare)", "Miffy Flameset Blue (Legendary)", "Miffy Flameset Purple (Legendary)", "Miffy Flameset Red (Legendary)",
                "Miffy Flameset Yellow (Legendary)", "Miffy STEPN Blue (Common)", "Miffy STEPN Green (Common)", "Miffy STEPN Purple (Common)",
                "Miffy STEPN Red (Common)", "Miffy VR Blue (Common)", "Miffy VR Green (Common)", "Miffy VR Pink (Common)",
                "Miffy VR Red (Common)", "Mog Armour Blue (Rare)", "Mog Armour Green (Rare)", "Mog Armour Purple (Rare)",
                "Mog Armour Yellow (Rare)", "Mog Flameset Blue (Legendary)", "Mog Flameset Purple (Legendary)", "Mog Flameset Red (Legendary)",
                "Mog Flameset Yellow (Legendary)", "Mog STEPN Blue (Common)", "Mog STEPN Green (Common)", "Mog STEPN Orange (Common)",
                "Mog STEPN Pink (Common)", "Mog VR Blue (Common)", "Mog VR Green (Common)", "Mog VR Orange (Common)",
                "Mog VR Pink (Common)", "Mooarty ADL Purple (Common)", "Mooarty Armour Blue (Rare)", "Mooarty Armour Green (Rare)",
                "Mooarty Armour Purple (Rare)", "Mooarty Armour Red (Rare)", "Mooarty Flameset Blue (Legendary)", "Mooarty Flameset Purple (Legendary)",
                "Mooarty Flameset Red (Legendary)", "Mooarty Flameset Yellow (Legendary)", "Mooarty Mid-Autumn Orange (Common)", "Mooarty STEPN Blue (Common)",
                "Mooarty STEPN Brown (Common)", "Mooarty STEPN Grey (Common)", "Mooarty STEPN Purple (Common)", "Mooarty VR Blue (Common)",
                "Mooarty VR Green (Common)", "Mooarty VR Purple (Common)", "Mooarty VR Yellow (Common)", "Mooarty Vampire Black (Common)",
                "Mooarty Zombie Green (Common)", "Spesh Coolman Spesh Grey (Rare)"
        ));
        defs2.put("Headgear", TraitDef.enumOf("Headgear", "v8",
                "Iggy Coolman Iggy Pink (Rare)", "Marty Armour Blue (Rare)", "Marty Armour Green (Rare)", "Marty Armour Purple (Rare)",
                "Marty Armour Yellow (Rare)", "Marty Flameset Blue (Legendary)", "Marty Flameset Green (Legendary)", "Marty Flameset Purple (Legendary)",
                "Marty Flameset Yellow (Legendary)", "Marty STEPN Blue (Common)", "Marty STEPN Green (Common)", "Marty STEPN Grey (Common)",
                "Marty STEPN Red (Common)", "Marty VR Pink (Common)", "Marty VR Purple (Common)", "Marty VR Red (Common)",
                "Marty VR Yellow (Common)", "Miffy Armour Blue (Rare)", "Miffy Armour Green (Rare)", "Miffy Armour Purple (Rare)",
                "Miffy Armour Yellow (Rare)", "Miffy Flameset Blue (Legendary)", "Miffy Flameset Orange (Legendary)", "Miffy Flameset Purple (Legendary)",
                "Miffy Flameset Red (Legendary)", "Miffy STEPN Blue (Common)", "Miffy STEPN Green (Common)", "Miffy STEPN Purple (Common)",
                "Miffy STEPN Red (Common)", "Miffy VR Blue (Common)", "Miffy VR Green (Common)", "Miffy VR Pink (Common)",
                "Miffy VR Red (Common)", "Mog Armour Blue (Rare)", "Mog Armour Green (Rare)", "Mog Armour Purple (Rare)",
                "Mog Armour Yellow (Rare)", "Mog Flameset Blue (Legendary)", "Mog Flameset Orange (Legendary)", "Mog Flameset Purple (Legendary)",
                "Mog Flameset Red (Legendary)", "Mog STEPN Blue (Common)", "Mog STEPN Green (Common)", "Mog STEPN Pink (Common)",
                "Mog STEPN Red (Common)", "Mog VR Blue (Common)", "Mog VR Green (Common)", "Mog VR Orange (Common)",
                "Mog VR Pink (Common)", "Mooarty ADL Purple (Common)", "Mooarty Armour Blue (Rare)", "Mooarty Armour Green (Rare)",
                "Mooarty Armour Purple (Rare)", "Mooarty Armour Yellow (Rare)", "Mooarty Flameset Blue (Legendary)", "Mooarty Flameset Green (Legendary)",
                "Mooarty Flameset Orange (Legendary)", "Mooarty Flameset Purple (Legendary)", "Mooarty Flameset Red (Legendary)", "Mooarty Mid-Autumn Orange (Common)",
                "Mooarty STEPN Blue (Common)", "Mooarty STEPN Grey (Common)", "Mooarty STEPN Purple (Common)", "Mooarty STEPN Yellow (Common)",
                "Mooarty VR Green (Common)", "Mooarty VR Grey (Common)", "Mooarty VR Purple (Common)", "Mooarty VR Red (Common)",
                "Mooarty Vampire Black (Common)", "Mooarty Zombie Green (Common)", "Spesh Coolman Spesh Pink (Rare)"
        ));
        defs2.put("Sneaker", TraitDef.enumOf("Sneaker", "v9",
                "Iggy Coolman Iggy Pink (Rare)", "Marty Armour Blue (Rare)", "Marty Armour Green (Rare)", "Marty Armour Purple (Rare)",
                "Marty Armour Yellow (Rare)", "Marty Flameset Blue (Legendary)", "Marty Flameset Orange (Legendary)", "Marty Flameset Purple (Legendary)",
                "Marty Flameset Red (Legendary)", "Marty STEPN Blue (Common)", "Marty STEPN Green (Common)", "Marty STEPN Red (Common)",
                "Marty STEPN Yellow (Common)", "Marty VR Pink (Common)", "Marty VR Purple (Common)", "Marty VR Red (Common)",
                "Marty VR Yellow (Common)", "Miffy Armour Blue (Rare)", "Miffy Armour Green (Rare)", "Miffy Armour Purple (Rare)",
                "Miffy Armour Yellow (Rare)", "Miffy Flameset Blue (Legendary)", "Miffy Flameset Orange (Legendary)", "Miffy Flameset Purple (Legendary)",
                "Miffy Flameset Red (Legendary)", "Miffy STEPN Blue (Common)", "Miffy STEPN Green (Common)", "Miffy STEPN Orange (Common)",
                "Miffy STEPN Red (Common)", "Miffy VR Blue (Common)", "Miffy VR Green (Common)", "Miffy VR Pink (Common)",
                "Miffy VR Red (Common)", "Mog Armour Blue (Rare)", "Mog Armour Green (Rare)", "Mog Armour Purple (Rare)",
                "Mog Armour Yellow (Rare)", "Mog Flameset Blue (Legendary)", "Mog Flameset Purple (Legendary)", "Mog Flameset Red (Legendary)",
                "Mog Flameset Yellow (Legendary)", "Mog STEPN Blue (Common)", "Mog STEPN Green (Common)", "Mog STEPN Pink (Common)",
                "Mog STEPN Red (Common)", "Mog VR Blue (Common)", "Mog VR Green (Common)", "Mog VR Orange (Common)",
                "Mog VR Pink (Common)", "Mooarty ADL Purple (Common)", "Mooarty Armour Blue (Rare)", "Mooarty Armour Purple (Rare)",
                "Mooarty Armour Red (Rare)", "Mooarty Armour Yellow (Rare)", "Mooarty Flamset Blue (Legendary)", "Mooarty Flamset Orange (Legendary)",
                "Mooarty Flamset Purple (Legendary)", "Mooarty Flamset Red (Legendary)", "Mooarty Mid-Autumn Orange (Common)", "Mooarty STEPN Blue (Common)",
                "Mooarty STEPN Green (Common)", "Mooarty STEPN Orange (Common)", "Mooarty STEPN Red (Common)", "Mooarty VR Blue (Common)",
                "Mooarty VR Green (Common)", "Mooarty VR Purple (Common)", "Mooarty VR Yellow (Common)", "Mooarty Vampire Black (Common)",
                "Mooarty Zombie Green (Common)", "Spesh Coolman Spesh Pink (Rare)"
        ));
        defs2.put("Sticker", TraitDef.enumOf("Sticker", "v10",
                "Marty Armour (Rare)", "Miffy Armour (Rare)", "Mog Armour (Rare)", "Mooarty Armour (Rare)"
        ));
        defs2.put("State", TraitDef.enumOf("State", "v11",
                "Unlock"
        ));
        DICT.put("0xb48f53010acbc0e24d3d12d892e2215879e6fd13", defs2);

        Map<String, TraitDef> defs3 = new LinkedHashMap<>();
        defs3.put("Rate Up", TraitDef.enumOf("Rate Up", "v1",
                "100%", "150%", "50%", "No"
        ));
        defs3.put("Tradable", TraitDef.enumOf("Tradable", "v2",
                "No", "Yes"
        ));
        DICT.put("0x5b00646169fbc1487c472f1dc28de09c5d350ced", defs3);

        Map<String, TraitDef> defs4 = new LinkedHashMap<>();
        defs4.put("Event", TraitDef.enumOf("Event", "v1",
                "BidMOOAR", "Easter Egg Hunt", "Energy War", "FSL Festive Points Chase",
                "Halloween GGBox"
        ));
        DICT.put("0xa80739520dfd78c4d4dfcb677bb3877f93fe6aca", defs4);

        Map<String, TraitDef> defs5 = new LinkedHashMap<>();
        defs5.put("Event", TraitDef.enumOf("Event", "v1",
                "BidMOOAR", "Easter Egg Hunt", "Energy War", "FSL Festive Points Chase",
                "Halloween GGBox"
        ));
        DICT.put("0xf51e00007e1d43503978ecc21c25e4ffdaa9e71e", defs5);
    }

    private TraitDict() {
    }

    /**
     * 取某合约的 trait 定义（属性名 -> 定义），未配置返回空表
     */
    public static Map<String, TraitDef> get(String contract) {
        if (contract == null || contract.isBlank()) {
            return Collections.emptyMap();
        }
        Map<String, TraitDef> defs = DICT.get(contract.toLowerCase());
        return defs == null ? Collections.emptyMap() : defs;
    }
}
