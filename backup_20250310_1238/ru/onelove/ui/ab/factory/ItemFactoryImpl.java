package ru.onelove.ui.ab.factory;

import ru.onelove.ui.ab.model.IItem;
import ru.onelove.ui.ab.model.ItemImpl;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;

import java.util.Map;

public class ItemFactoryImpl implements ItemFactory {
    @Override
    public IItem createNewItem(Item item, int price, int quantity, int damage, Map<Enchantment, Integer> enchantments) {
        return new ItemImpl(item, price, quantity, damage, enchantments);
    }
}
