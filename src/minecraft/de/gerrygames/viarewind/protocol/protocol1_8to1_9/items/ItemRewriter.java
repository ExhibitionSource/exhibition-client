//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package de.gerrygames.viarewind.protocol.protocol1_8to1_9.items;

import de.gerrygames.viarewind.protocol.protocol1_8to1_9.Protocol1_8TO1_9;
import de.gerrygames.viarewind.utils.Enchantments;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.enchantment.Enchantment;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ShortTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

public class ItemRewriter {
    private static Map<String, Integer> ENTTIY_NAME_TO_ID;
    private static Map<Integer, String> ENTTIY_ID_TO_NAME;
    private static Map<String, Integer> POTION_NAME_TO_ID;
    private static Map<Integer, String> POTION_ID_TO_NAME;
    private static Map<String, String> POTION_NAME_INDEX = new HashMap();

    public ItemRewriter() {
    }

    public static Item toClient(Item item) {
        if (item == null) {
            return null;
        } else {
            CompoundTag tag = item.getTag();
            if (tag == null) {
                item.setTag(tag = new CompoundTag(""));
            }

            CompoundTag viaVersionTag = new CompoundTag("ViaRewind1_8to1_9");
            tag.put(viaVersionTag);
            viaVersionTag.put(new ShortTag("id", (short)item.getIdentifier()));
            viaVersionTag.put(new ShortTag("data", item.getData()));
            CompoundTag display = (CompoundTag)tag.get("display");
            if (display != null && display.contains("Name")) {
                viaVersionTag.put(new StringTag("displayName", (String)display.get("Name").getValue()));
            }

            if (display != null && display.contains("Lore")) {
                viaVersionTag.put(new ListTag("lore", ((ListTag)display.get("Lore")).getValue()));
            }

            ListTag attributes;
            if (tag.contains("ench") || tag.contains("StoredEnchantments")) {
                attributes = tag.contains("ench") ? (ListTag)tag.get("ench") : (ListTag)tag.get("StoredEnchantments");
                List<Tag> enchants = attributes.getValue();
                List<Tag> lore = new ArrayList();
                Iterator var7 = enchants.iterator();

                label162:
                while(true) {
                    Tag ench;
                    short lvl;
                    String s;
                    while(true) {
                        if (!var7.hasNext()) {
                            if (!lore.isEmpty()) {
                                if (display == null) {
                                    tag.put(display = new CompoundTag("display"));
                                    viaVersionTag.put(new ByteTag("noDisplay"));
                                }

                                ListTag loreTag = (ListTag)display.get("Lore");
                                if (loreTag == null) {
                                    display.put(loreTag = new ListTag("Lore", StringTag.class));
                                }

                                lore.addAll(loreTag.getValue());
                                loreTag.setValue(lore);
                            }
                            break label162;
                        }

                        ench = (Tag)var7.next();

                        short id;
                        if (((CompoundTag)ench).get("id").getValue() instanceof String){
                            id = Short.valueOf(String.valueOf((Enchantment.getEnchantmentByLocation((String) ((CompoundTag)ench).get("id").getValue()).effectId)));
                        } else {
                            id = Short.valueOf(String.valueOf(((CompoundTag)ench).get("id").getValue()));
                        }

                        lvl = Short.valueOf(String.valueOf(((CompoundTag)ench).get("lvl").getValue()));
                        if (id == 70) {
                            s = "§r§7Mending ";
                            break;
                        }

                        if (id == 9) {
                            s = "§r§7Frost Walker ";
                            break;
                        }
                    }

                    attributes.remove(ench);
                    s = s + (String)Enchantments.ENCHANTMENTS.getOrDefault(lvl, "enchantment.level." + lvl);
                    lore.add(new StringTag("", s));
                }
            }

            if (item.getData() != 0 && tag.contains("Unbreakable")) {
                ByteTag unbreakable = (ByteTag)tag.get("Unbreakable");
                if (unbreakable.getValue() != 0) {
                    viaVersionTag.put(new ByteTag("Unbreakable", unbreakable.getValue()));
                    tag.remove("Unbreakable");
                    if (display == null) {
                        tag.put(display = new CompoundTag("display"));
                        viaVersionTag.put(new ByteTag("noDisplay"));
                    }

                    ListTag loreTag = (ListTag)display.get("Lore");
                    if (loreTag == null) {
                        display.put(loreTag = new ListTag("Lore", StringTag.class));
                    }

                    loreTag.add(new StringTag("", "§9Unbreakable"));
                }
            }

            if (tag.contains("AttributeModifiers")) {
                viaVersionTag.put(tag.get("AttributeModifiers").clone());
            }

            int data;
            if (item.getIdentifier() == 383 && item.getData() == 0) {
                data = 0;
                if (tag.contains("EntityTag")) {
                    CompoundTag entityTag = (CompoundTag)tag.get("EntityTag");
                    if (entityTag.contains("id")) {
                        StringTag id = (StringTag)entityTag.get("id");
                        if (ENTTIY_NAME_TO_ID.containsKey(id.getValue())) {
                            data = (Integer)ENTTIY_NAME_TO_ID.get(id.getValue());
                        } else if (display == null) {
                            tag.put(display = new CompoundTag("display"));
                            viaVersionTag.put(new ByteTag("noDisplay"));
                            display.put(new StringTag("Name", "§rSpawn " + id.getValue()));
                        }
                    }
                }

                item.setData((short)data);
            }

            ReplacementRegistry1_8to1_9.replace(item);
            if (item.getIdentifier() == 373 || item.getIdentifier() == 438 || item.getIdentifier() == 441) {
                data = 0;
                if (tag.contains("Potion")) {
                    StringTag potion = (StringTag)tag.get("Potion");
                    String potionName = potion.getValue().replace("minecraft:", "");
                    if (POTION_NAME_TO_ID.containsKey(potionName)) {
                        data = (Integer)POTION_NAME_TO_ID.get(potionName);
                    }

                    if (item.getIdentifier() == 438) {
                        potionName = potionName + "_splash";
                    } else if (item.getIdentifier() == 441) {
                        potionName = potionName + "_lingering";
                    }

                    if ((display == null || !display.contains("Name")) && POTION_NAME_INDEX.containsKey(potionName)) {
                        if (display == null) {
                            tag.put(display = new CompoundTag("display"));
                            viaVersionTag.put(new ByteTag("noDisplay"));
                        }

                        display.put(new StringTag("Name", (String)POTION_NAME_INDEX.get(potionName)));
                    }
                }

                if (item.getIdentifier() == 438 || item.getIdentifier() == 441) {
                    item.setIdentifier(373);
                    data += 8192;
                }

                item.setData((short)data);
            }

            if (tag.contains("AttributeModifiers")) {
                attributes = (ListTag)tag.get("AttributeModifiers");

                for(int i = 0; i < attributes.size(); ++i) {
                    CompoundTag attribute = (CompoundTag)attributes.get(i);
                    String name = (String)attribute.get("AttributeName").getValue();
                    if (!Protocol1_8TO1_9.VALID_ATTRIBUTES.contains(attribute)) {
                        attributes.remove(attribute);
                        --i;
                    }
                }
            }

            if (viaVersionTag.size() == 2 && (Short)viaVersionTag.get("id").getValue() == item.getIdentifier() && (Short)viaVersionTag.get("data").getValue() == item.getData()) {
                item.getTag().remove("ViaRewind1_8to1_9");
                if (item.getTag().isEmpty()) {
                    item.setTag((CompoundTag)null);
                }
            }

            return item;
        }
    }

    public static Item toServer(Item item) {
        if (item == null) {
            return null;
        } else {
            CompoundTag tag = item.getTag();
            CompoundTag viaVersionTag;
            if (item.getIdentifier() == 383 && item.getData() != 0) {
                if (tag == null) {
                    item.setTag(tag = new CompoundTag(""));
                }

                if (!tag.contains("EntityTag") && ENTTIY_ID_TO_NAME.containsKey(Integer.valueOf(item.getData()))) {
                    viaVersionTag = new CompoundTag("EntityTag");
                    viaVersionTag.put(new StringTag("id", (String)ENTTIY_ID_TO_NAME.get(Integer.valueOf(item.getData()))));
                    tag.put(viaVersionTag);
                }

                item.setData((short)0);
            }

            if (item.getIdentifier() == 373 && (tag == null || !tag.contains("Potion"))) {
                if (tag == null) {
                    item.setTag(tag = new CompoundTag(""));
                }

                if (item.getData() >= 16384) {
                    item.setIdentifier(438);
                    item.setData((short)(item.getData() - 8192));
                }

                String name = item.getData() == 8192 ? "water" : us.myles.ViaVersion.protocols.protocol1_9to1_8.ItemRewriter.potionNameFromDamage(item.getData());
                tag.put(new StringTag("Potion", "minecraft:" + name));
                item.setData((short)0);
            }

            if (tag != null && item.getTag().contains("ViaRewind1_8to1_9")) {
                viaVersionTag = (CompoundTag)tag.remove("ViaRewind1_8to1_9");
                item.setIdentifier((Short)viaVersionTag.get("id").getValue());
                item.setData((Short)viaVersionTag.get("data").getValue());
                if (viaVersionTag.contains("noDisplay")) {
                    tag.remove("display");
                }

                if (viaVersionTag.contains("Unbreakable")) {
                    tag.put(viaVersionTag.get("Unbreakable").clone());
                }

                CompoundTag display;
                if (viaVersionTag.contains("displayName")) {
                    display = (CompoundTag)tag.get("display");
                    if (display == null) {
                        tag.put(display = new CompoundTag("display"));
                    }

                    StringTag name = (StringTag)display.get("Name");
                    if (name == null) {
                        display.put(new StringTag("Name", (String)viaVersionTag.get("displayName").getValue()));
                    } else {
                        name.setValue((String)viaVersionTag.get("displayName").getValue());
                    }
                } else if (tag.contains("display")) {
                    ((CompoundTag)tag.get("display")).remove("Name");
                }

                if (viaVersionTag.contains("lore")) {
                    display = (CompoundTag)tag.get("display");
                    if (display == null) {
                        tag.put(display = new CompoundTag("display"));
                    }

                    ListTag lore = (ListTag)display.get("Lore");
                    if (lore == null) {
                        display.put(new ListTag("Lore", (List)viaVersionTag.get("lore").getValue()));
                    } else {
                        lore.setValue((List)viaVersionTag.get("lore").getValue());
                    }
                } else if (tag.contains("display")) {
                    ((CompoundTag)tag.get("display")).remove("Lore");
                }

                tag.remove("AttributeModifiers");
                if (viaVersionTag.contains("AttributeModifiers")) {
                    tag.put(viaVersionTag.get("AttributeModifiers"));
                }

                return item;
            } else {
                return item;
            }
        }
    }

    static {
        Field[] var0 = ItemRewriter.class.getDeclaredFields();
        int var1 = var0.length;

        for(int var2 = 0; var2 < var1; ++var2) {
            Field field = var0[var2];

            try {
                Field other = us.myles.ViaVersion.protocols.protocol1_9to1_8.ItemRewriter.class.getDeclaredField(field.getName());
                other.setAccessible(true);
                field.setAccessible(true);
                field.set((Object)null, other.get((Object)null));
            } catch (Exception var5) {
            }
        }

        POTION_NAME_TO_ID.put("luck", 8203);
        POTION_NAME_INDEX.put("water", "§rWater Bottle");
        POTION_NAME_INDEX.put("mundane", "§rMundane Potion");
        POTION_NAME_INDEX.put("thick", "§rThick Potion");
        POTION_NAME_INDEX.put("awkward", "§rAwkward Potion");
        POTION_NAME_INDEX.put("water_splash", "§rSplash Water Bottle");
        POTION_NAME_INDEX.put("mundane_splash", "§rMundane Splash Potion");
        POTION_NAME_INDEX.put("thick_splash", "§rThick Splash Potion");
        POTION_NAME_INDEX.put("awkward_splash", "§rAwkward Splash Potion");
        POTION_NAME_INDEX.put("water_lingering", "§rLingering Water Bottle");
        POTION_NAME_INDEX.put("mundane_lingering", "§rMundane Lingering Potion");
        POTION_NAME_INDEX.put("thick_lingering", "§rThick Lingering Potion");
        POTION_NAME_INDEX.put("awkward_lingering", "§rAwkward Lingering Potion");
        POTION_NAME_INDEX.put("night_vision_lingering", "§rLingering Potion of Night Vision");
        POTION_NAME_INDEX.put("long_night_vision_lingering", "§rLingering Potion of Night Vision");
        POTION_NAME_INDEX.put("invisibility_lingering", "§rLingering Potion of Invisibility");
        POTION_NAME_INDEX.put("long_invisibility_lingering", "§rLingering Potion of Invisibility");
        POTION_NAME_INDEX.put("leaping_lingering", "§rLingering Potion of Leaping");
        POTION_NAME_INDEX.put("long_leaping_lingering", "§rLingering Potion of Leaping");
        POTION_NAME_INDEX.put("strong_leaping_lingering", "§rLingering Potion of Leaping");
        POTION_NAME_INDEX.put("fire_resistance_lingering", "§rLingering Potion of Fire Resistance");
        POTION_NAME_INDEX.put("long_fire_resistance_lingering", "§rLingering Potion of Fire Resistance");
        POTION_NAME_INDEX.put("swiftness_lingering", "§rLingering Potion of Swiftness");
        POTION_NAME_INDEX.put("long_swiftness_lingering", "§rLingering Potion of Swiftness");
        POTION_NAME_INDEX.put("strong_swiftness_lingering", "§rLingering Potion of Swiftness");
        POTION_NAME_INDEX.put("slowness_lingering", "§rLingering Potion of Slowness");
        POTION_NAME_INDEX.put("long_slowness_lingering", "§rLingering Potion of Slowness");
        POTION_NAME_INDEX.put("water_breathing_lingering", "§rLingering Potion of Water Breathing");
        POTION_NAME_INDEX.put("long_water_breathing_lingering", "§rLingering Potion of Water Breathing");
        POTION_NAME_INDEX.put("healing_lingering", "§rLingering Potion of Healing");
        POTION_NAME_INDEX.put("strong_healing_lingering", "§rLingering Potion of Healing");
        POTION_NAME_INDEX.put("harming_lingering", "§rLingering Potion of Harming");
        POTION_NAME_INDEX.put("strong_harming_lingering", "§rLingering Potion of Harming");
        POTION_NAME_INDEX.put("poison_lingering", "§rLingering Potion of Poisen");
        POTION_NAME_INDEX.put("long_poison_lingering", "§rLingering Potion of Poisen");
        POTION_NAME_INDEX.put("strong_poison_lingering", "§rLingering Potion of Poisen");
        POTION_NAME_INDEX.put("regeneration_lingering", "§rLingering Potion of Regeneration");
        POTION_NAME_INDEX.put("long_regeneration_lingering", "§rLingering Potion of Regeneration");
        POTION_NAME_INDEX.put("strong_regeneration_lingering", "§rLingering Potion of Regeneration");
        POTION_NAME_INDEX.put("strength_lingering", "§rLingering Potion of Strength");
        POTION_NAME_INDEX.put("long_strength_lingering", "§rLingering Potion of Strength");
        POTION_NAME_INDEX.put("strong_strength_lingering", "§rLingering Potion of Strength");
        POTION_NAME_INDEX.put("weakness_lingering", "§rLingering Potion of Weakness");
        POTION_NAME_INDEX.put("long_weakness_lingering", "§rLingering Potion of Weakness");
        POTION_NAME_INDEX.put("luck_lingering", "§rLingering Potion of Luck");
        POTION_NAME_INDEX.put("luck", "§rPotion of Luck");
        POTION_NAME_INDEX.put("luck_splash", "§rSplash Potion of Luck");
    }
}
