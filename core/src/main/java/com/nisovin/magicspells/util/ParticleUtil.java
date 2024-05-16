package com.nisovin.magicspells.util;

import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.NamespacedKey;

public class ParticleUtil {

	public static Particle getParticle(String particleName) {
		String lower = particleName.toLowerCase();

		return switch (lower) {
			case "explode", "explosion_normal" -> Particle.POOF;
			case "largeexplode", "explosion_large" -> Particle.EXPLOSION;
			case "hugeexplosion", "explosion_huge" -> Particle.EXPLOSION_EMITTER;
			case "fireworksspark", "fireworks_spark" -> Particle.FIREWORK;
			case "water_bubble" -> Particle.BUBBLE;
			case "water_splash" -> Particle.SPLASH;
			case "wake", "water_wake" -> Particle.FISHING;
			case "depthsuspend", "suspended_depth" -> Particle.UNDERWATER;
			case "magiccrit", "crit_magic" -> Particle.ENCHANTED_HIT;
			case "smoke_normal" -> Particle.SMOKE;
			case "largesmoke", "smoke_large" -> Particle.LARGE_SMOKE;
			case "spell" -> Particle.EFFECT;
			case "instantspell", "spell_instant" -> Particle.INSTANT_EFFECT;
			case "mobspell", "mobspellambient", "spell_mob_ambient", "spell_mob" -> Particle.ENTITY_EFFECT;
			case "witchmagic", "spell_witch" -> Particle.WITCH;
			case "dripwater", "drip_water" -> Particle.DRIPPING_WATER;
			case "driplava", "drip_lava" -> Particle.DRIPPING_LAVA;
			case "angryvillager", "villager_angry" -> Particle.ANGRY_VILLAGER;
			case "happyvillager", "villager_happy" -> Particle.HAPPY_VILLAGER;
			case "townaura", "town_aura" -> Particle.MYCELIUM;
			case "enchantmenttable", "enchantment_table" -> Particle.ENCHANT;
			case "reddust", "redstone" -> Particle.DUST;
			case "snowballpoof", "snowshovel", "snow_shovel", "snowball" -> Particle.ITEM_SNOWBALL;
			case "slime" -> Particle.ITEM_SLIME;
			case "iconcrack", "item_crack" -> Particle.ITEM;
			case "blockcrack", "blockdust", "block_dust", "block_crack" -> Particle.BLOCK;
			case "droplet", "water_drop" -> Particle.RAIN;
			case "mobappearance", "mob_appearance" -> Particle.ELDER_GUARDIAN;
			case "dragonbreath" -> Particle.DRAGON_BREATH;
			case "endrod" -> Particle.END_ROD;
			case "damageindicator" -> Particle.DAMAGE_INDICATOR;
			case "sweepattack" -> Particle.SWEEP_ATTACK;
			case "fallingdust" -> Particle.FALLING_DUST;
			case "totem" -> Particle.TOTEM_OF_UNDYING;
			default -> {
				try {
					yield Particle.valueOf(particleName.toUpperCase());
				} catch (IllegalArgumentException ignored) {}

				NamespacedKey key = NamespacedKey.fromString(lower);
				yield key != null ? Registry.PARTICLE_TYPE.get(key) : null;
			}
		};
	}

}
