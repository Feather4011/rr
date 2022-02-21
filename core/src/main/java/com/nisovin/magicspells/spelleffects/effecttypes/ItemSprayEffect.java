package com.nisovin.magicspells.spelleffects.effecttypes;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.SpellEffect;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

public class ItemSprayEffect extends SpellEffect {

	private Material material;
	private String materialName;
	private ItemStack itemStack;

	private ConfigData<Double> force;

	private ConfigData<Integer> amount;
	private ConfigData<Integer> duration;

	private boolean resolveForcePerItem;

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		materialName = config.getString("type", "");
		material = Util.getMaterial(materialName);

		if (material == null || !material.isItem()) {
			MagicSpells.error("Wrong type defined! '" + materialName + "'");
			itemStack = null;

			return;
		}
		itemStack = new ItemStack(material);

		force = ConfigDataUtil.getDouble(config, "force", 1);

		amount = ConfigDataUtil.getInteger(config, "amount", 15);
		duration = ConfigDataUtil.getInteger(config, "duration", 10);

		resolveForcePerItem = config.getBoolean("resolve-force-per-item", false);
	}

	@Override
	public Runnable playEffectLocation(Location location, SpellData data) {
		if (itemStack == null) return null;

		Random rand = ThreadLocalRandom.current();
		Location loc = location.clone().add(0, 1, 0);

		double force = resolveForcePerItem ? 0 : this.force.get(data);
		int duration = this.duration.get(data);

		int amount = this.amount.get(data);
		Item[] items = new Item[amount];
		for (int i = 0; i < amount; i++) {
			items[i] = loc.getWorld().dropItem(loc, itemStack);

			if (resolveForcePerItem) force = this.force.get(data);
			items[i].setVelocity(new Vector((rand.nextDouble() - 0.5d) * force, (rand.nextDouble() - 0.5d) * force, (rand.nextDouble() - 0.5d) * force));
			items[i].setPickupDelay(duration << 1);
		}

		MagicSpells.scheduleDelayedTask(() -> Arrays.stream(items).forEach(Item::remove), duration);
		return null;
	}

}
