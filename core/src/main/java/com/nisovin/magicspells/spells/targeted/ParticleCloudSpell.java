package com.nisovin.magicspells.spells.targeted;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.Particle.DustOptions;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.potion.PotionEffectType;

import net.kyori.adventure.text.Component;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.config.ConfigDataUtil;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.handlers.PotionEffectHandler;

public class ParticleCloudSpell extends TargetedSpell implements TargetedLocationSpell, TargetedEntitySpell {

	private final ConfigData<Vector> relativeOffset;

	private final ConfigData<Component> customName;

	protected ConfigData<ItemStack> item;
	protected ConfigData<Particle> particle;
	protected ConfigData<BlockData> blockData;
	protected ConfigData<DustOptions> dustOptions;

	private final ConfigData<Integer> color;
	private final ConfigData<Integer> waitTime;
	private final ConfigData<Integer> ticksDuration;
	private final ConfigData<Integer> durationOnUse;
	private final ConfigData<Integer> reapplicationDelay;

	private final ConfigData<Float> radius;
	private final ConfigData<Float> radiusOnUse;
	private final ConfigData<Float> radiusPerTick;

	private final ConfigData<Boolean> useGravity;
	private final ConfigData<Boolean> canTargetEntities;
	private final ConfigData<Boolean> canTargetLocation;

	private final Set<PotionEffect> potionEffects;

	public ParticleCloudSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		relativeOffset = getConfigDataVector("relative-offset", new Vector(0, 0.5, 0));

		customName = getConfigDataComponent("custom-name", null);

		particle = ConfigDataUtil.getParticle(config.getMainConfig(), internalKey + "particle", Particle.POOF);

		blockData = getConfigDataBlockData("material", null);
		dustOptions = ConfigDataUtil.getDustOptions(config.getMainConfig(), internalKey + "dust-color", internalKey + "size", new DustOptions(Color.RED, 1));

		ConfigData<Material> material = getConfigDataMaterial("material", null);
		if (material.isConstant()) {
			Material mat = material.get();

			ItemStack stack = mat != null && mat.isItem() ? new ItemStack(mat) : null;
			item = data -> stack;
		} else {
			item = data -> {
				Material mat = material.get(data);
				return mat != null && mat.isItem() ? new ItemStack(mat) : null;
			};
		}

		color = getConfigDataInt("color", 0xFF0000);
		waitTime = getConfigDataInt("wait-time-ticks", 10);
		ticksDuration = getConfigDataInt("duration-ticks", 3 * TimeUtil.TICKS_PER_SECOND);
		durationOnUse = getConfigDataInt("duration-ticks-on-use", 0);
		reapplicationDelay = getConfigDataInt("reapplication-delay-ticks", 3 * TimeUtil.TICKS_PER_SECOND);

		radius = getConfigDataFloat("radius", 5F);
		radiusOnUse = getConfigDataFloat("radius-on-use", 0F);
		radiusPerTick = getConfigDataFloat("radius-per-tick", 0F);

		useGravity = getConfigDataBoolean("use-gravity", false);
		canTargetEntities = getConfigDataBoolean("can-target-entities", true);
		canTargetLocation = getConfigDataBoolean("can-target-location", true);

		List<String> potionEffectStrings = getConfigStringList("potion-effects", null);
		if (potionEffectStrings == null) potionEffectStrings = new ArrayList<>();

		potionEffects = new HashSet<>();

		for (String effect : potionEffectStrings) {
			potionEffects.add(getPotionEffectFromString(effect));
		}
	}

	private static PotionEffect getPotionEffectFromString(String s) {
		String[] splits = s.split(" ");
		PotionEffectType type = PotionEffectHandler.getPotionEffectType(splits[0]);

		int durationTicks = Integer.parseInt(splits[1]);
		int amplifier = Integer.parseInt(splits[2]);

		boolean ambient = Boolean.parseBoolean(splits[3]);
		boolean particles = Boolean.parseBoolean(splits[4]);
		boolean icon = Boolean.parseBoolean(splits[5]);

		return new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon);
	}

	@Override
	public CastResult cast(SpellData data) {
		if (canTargetEntities.get(data)) {
			TargetInfo<LivingEntity> info = getTargetedEntity(data);
			if (info.cancelled()) return noTarget(info);

			if (!info.noTarget()) {
				Location location = info.target().getLocation();
				location.setDirection(data.caster().getLocation().getDirection());
				data = info.spellData().location(location);

				spawnCloud(data);
				return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
			}
		}

		if (canTargetLocation.get(data)) {
			TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, 1, 0.5, false);
			if (info.noTarget()) return noTarget(info);
			data = info.spellData();

			spawnCloud(data);
			return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
		}

		return noTarget(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		spawnCloud(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		data = data.location(data.target().getLocation());
		spawnCloud(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	private void spawnCloud(SpellData data) {
		Location location = data.location();

		//apply relative offset
		Vector relativeOffset = this.relativeOffset.get(data);
		location.add(0, relativeOffset.getY(), 0);
		Util.applyRelativeOffset(location, relativeOffset.setY(0));

		data = data.location(location);

		SpellData finalData = data;
		location.getWorld().spawn(location, AreaEffectCloud.class, cloud -> {
			Particle particle = this.particle.get(finalData);

			Class<?> dataType = particle.getDataType();
			if (dataType == BlockData.class) cloud.setParticle(particle, blockData.get(finalData));
			else if (dataType == ItemStack.class) cloud.setParticle(particle, item.get(finalData));
			else if (dataType == DustOptions.class) cloud.setParticle(particle, dustOptions.get(finalData));
			else cloud.setParticle(particle);

			cloud.setColor(Color.fromRGB(color.get(finalData)));
			cloud.setRadius(radius.get(finalData));
			cloud.setGravity(useGravity.get(finalData));
			cloud.setWaitTime(waitTime.get(finalData));
			cloud.setDuration(ticksDuration.get(finalData));
			cloud.setDurationOnUse(durationOnUse.get(finalData));
			cloud.setRadiusOnUse(radiusOnUse.get(finalData));
			cloud.setRadiusPerTick(radiusPerTick.get(finalData));
			cloud.setReapplicationDelay(reapplicationDelay.get(finalData));

			for (PotionEffect eff : potionEffects) cloud.addCustomEffect(eff, true);

			if (customName != null) {
				cloud.customName(customName.get(finalData));
				cloud.setCustomNameVisible(true);
			}
		});

		if (data.hasTarget()) playSpellEffects(data.caster(), data.target(), data);
		else playSpellEffects(data.caster(), location, data);
	}

}
