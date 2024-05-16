package com.nisovin.magicspells.spells.targeted;

import org.bukkit.potion.PotionEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.spells.TargetedEntitySpell;

public class CrippleSpell extends TargetedSpell implements TargetedEntitySpell {

	private final ConfigData<Integer> strength;
	private final ConfigData<Integer> duration;
	private final ConfigData<Integer> portalCooldown;

	private final ConfigData<Boolean> useSlownessEffect;
	private final ConfigData<Boolean> applyPortalCooldown;
	private final ConfigData<Boolean> powerAffectsDuration;
	private final ConfigData<Boolean> powerAffectsPortalCooldown;

	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		strength = getConfigDataInt("effect-strength", 5);
		duration = getConfigDataInt("effect-duration", 100);
		portalCooldown = getConfigDataInt("portal-cooldown-ticks", 100);

		useSlownessEffect = getConfigDataBoolean("use-slowness-effect", true);
		applyPortalCooldown = getConfigDataBoolean("apply-portal-cooldown", false);
		powerAffectsDuration = getConfigDataBoolean("power-affects-duration", true);
		powerAffectsPortalCooldown = getConfigDataBoolean("power-affects-portal-cooldown", true);
	}

	@Override
	public CastResult cast(SpellData data) {
		TargetInfo<LivingEntity> info = getTargetedEntity(data);
		if (info.noTarget()) return noTarget(info);

		return castAtEntity(info.spellData());
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		if (useSlownessEffect.get(data)) {
			int strength = this.strength.get(data);
			int duration = this.duration.get(data);
			if (powerAffectsDuration.get(data)) duration = Math.round(duration * data.power());

			data.target().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, strength));
		}

		if (applyPortalCooldown.get(data)) {
			int portalCooldown = this.portalCooldown.get(data);
			if (powerAffectsPortalCooldown.get(data)) portalCooldown = Math.round(portalCooldown * data.power());

			if (data.target().getPortalCooldown() < portalCooldown) data.target().setPortalCooldown(portalCooldown);
		}

		playSpellEffects(data);
		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

}
