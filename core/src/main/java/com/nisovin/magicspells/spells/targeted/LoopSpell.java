package com.nisovin.magicspells.spells.targeted;

import java.util.*;

import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.*;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.managers.VariableManager;
import com.nisovin.magicspells.events.SpellTargetLocationEvent;

public class LoopSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private static DeathListener deathListener;

	private final Multimap<UUID, Loop> activeLoops = HashMultimap.create();

	private final ConfigData<Integer> iterations;

	private final ConfigData<Long> delay;
	private final ConfigData<Long> duration;
	private final ConfigData<Long> interval;

	private final ConfigData<Double> yOffset;

	private final ConfigData<Boolean> targeted;
	private final ConfigData<Boolean> pointBlank;
	private final ConfigData<Boolean> stopOnFail;
	private final ConfigData<Boolean> cancelOnDeath;
	private final ConfigData<Boolean> passTargeting;
	private final ConfigData<Boolean> stopOnSuccess;
	private final ConfigData<Boolean> requireEntityTarget;
	private final ConfigData<Boolean> castRandomSpellInstead;
	private final ConfigData<Boolean> skipFirstLoopModifiers;
	private final ConfigData<Boolean> skipFirstVariableModsLoop;
	private final ConfigData<Boolean> skipFirstLoopTargetModifiers;
	private final ConfigData<Boolean> skipFirstLoopLocationModifiers;
	private final ConfigData<Boolean> skipFirstVariableModsTargetLoop;

	private final String strFadeSelf;
	private final String strFadeTarget;

	private Subspell spellOnEnd;
	private String spellOnEndName;

	private List<Subspell> spells;
	private List<String> spellNames;

	private List<String> varModsLoop;
	private List<String> varModsTargetLoop;

	private Multimap<String, VariableMod> variableModsLoop;
	private Multimap<String, VariableMod> variableModsTargetLoop;

	private List<String> loopModifierStrings;
	private List<String> loopTargetModifierStrings;
	private List<String> loopLocationModifierStrings;

	private ModifierSet loopModifiers;
	private ModifierSet loopTargetModifiers;
	private ModifierSet loopLocationModifiers;

	public LoopSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		iterations = getConfigDataInt("iterations", 0);

		delay = getConfigDataLong("delay", 0);
		duration = getConfigDataLong("duration", 0);
		interval = getConfigDataLong("interval", 20);

		yOffset = getConfigDataDouble("y-offset", 0);

		targeted = getConfigDataBoolean("targeted", true);
		pointBlank = getConfigDataBoolean("point-blank", false);
		stopOnFail = getConfigDataBoolean("stop-on-fail", false);
		passTargeting = getConfigDataBoolean("pass-targeting", true);
		cancelOnDeath = getConfigDataBoolean("cancel-on-death", false);
		stopOnSuccess = getConfigDataBoolean("stop-on-success", false);
		requireEntityTarget = getConfigDataBoolean("require-entity-target", false);
		castRandomSpellInstead = getConfigDataBoolean("cast-random-spell-instead", false);

		ConfigData<Boolean> skipFirst = getConfigDataBoolean("skip-first", false);
		skipFirstLoopModifiers = getConfigDataBoolean("skip-first-loop-modifiers", skipFirst);
		skipFirstVariableModsLoop = getConfigDataBoolean("skip-first-variable-mods-loop", skipFirst);
		skipFirstLoopTargetModifiers = getConfigDataBoolean("skip-first-loop-target-modifiers", skipFirst);
		skipFirstLoopLocationModifiers = getConfigDataBoolean("skip-first-loop-location-modifiers", skipFirst);
		skipFirstVariableModsTargetLoop = getConfigDataBoolean("skip-first-variable-mods-target-loop", skipFirst);

		strFadeSelf = getConfigString("str-fade-self", "");
		strFadeTarget = getConfigString("str-fade-target", "");
		spellOnEndName = getConfigString("spell-on-end", null);

		spellNames = getConfigStringList("spells", null);
		varModsLoop = getConfigStringList("variable-mods-loop", null);
		varModsTargetLoop = getConfigStringList("variable-mods-target-loop", null);
		loopModifierStrings = getConfigStringList("loop-modifiers", null);
		loopTargetModifierStrings = getConfigStringList("loop-target-modifiers", null);
		loopLocationModifierStrings = getConfigStringList("loop-location-modifiers", null);
	}

	@Override
	public void initialize() {
		super.initialize();

		if (deathListener == null) {
			deathListener = new DeathListener();
			registerEvents(deathListener);
		}

		if (spellOnEndName != null) {
			spellOnEnd = new Subspell(spellOnEndName);
			if (!spellOnEnd.process()) {
				MagicSpells.error("LoopSpell '" + internalName + "' has an invalid spell-on-end '" + spellOnEndName + "' defined!");
				spellOnEnd = null;
			}
		}
		spellOnEndName = null;

		if (spellNames != null && !spellNames.isEmpty()) {
			spells = new ArrayList<>();

			for (String spellName : spellNames) {
				Subspell spell = new Subspell(spellName);
				if (!spell.process()) {
					MagicSpells.error("LoopSpell '" + internalName + "' has an invalid spell '" + spellName + "' defined!");
					continue;
				}

				spells.add(spell);
			}

			if (spells.isEmpty()) spells = null;
		}
		spellNames = null;
	}

	@Override
	protected void initializeVariables() {
		super.initializeVariables();

		if (varModsLoop != null && !varModsLoop.isEmpty()) {
			variableModsLoop = LinkedListMultimap.create();

			for (String s : varModsLoop) {
				try {
					String[] data = s.split(" ", 2);
					variableModsLoop.put(data[0], new VariableMod(data[1]));
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-loop option for spell '" + internalName + "': " + s);
				}
			}

			if (variableModsLoop.isEmpty()) variableModsLoop = null;
		}

		if (varModsTargetLoop != null && !varModsTargetLoop.isEmpty()) {
			variableModsTargetLoop = LinkedListMultimap.create();

			for (String s : varModsTargetLoop) {
				try {
					String[] data = s.split(" ", 2);
					variableModsTargetLoop.put(data[0], new VariableMod(data[1]));
				} catch (Exception e) {
					MagicSpells.error("Invalid variable-mods-target-loop option for spell '" + internalName + "': " + s);
				}
			}

			if (variableModsTargetLoop.isEmpty()) variableModsTargetLoop = null;
		}

		varModsLoop = null;
		varModsTargetLoop = null;
	}

	@Override
	protected void initializeModifiers() {
		super.initializeModifiers();

		if (loopModifierStrings != null && !loopModifierStrings.isEmpty())
			loopModifiers = new ModifierSet(loopModifierStrings, this);

		if (loopTargetModifierStrings != null && !loopTargetModifierStrings.isEmpty())
			loopTargetModifiers = new ModifierSet(loopTargetModifierStrings, this);

		if (loopLocationModifierStrings != null && !loopLocationModifierStrings.isEmpty())
			loopLocationModifiers = new ModifierSet(loopLocationModifierStrings, this);

		loopModifierStrings = null;
		loopTargetModifierStrings = null;
		loopLocationModifierStrings = null;
	}

	@Override
	public CastResult cast(SpellData data) {
		if (targeted.get(data)) {
			if (requireEntityTarget.get(data)) {
				TargetInfo<LivingEntity> info = getTargetedEntity(data);
				if (info.noTarget()) return noTarget(info);
				data = info.spellData();
			} else if (pointBlank.get(data)) {
				SpellTargetLocationEvent targetEvent = new SpellTargetLocationEvent(this, data, data.caster().getLocation());
				if (!targetEvent.callEvent()) return noTarget(targetEvent);
				data = targetEvent.getSpellData();
			} else {
				TargetInfo<Location> info = getTargetedBlockLocation(data, 0.5, yOffset.get(data) + 0.5, 0.5);
				if (info.noTarget()) return noTarget(info);
				data = info.spellData();
			}
		}

		return initLoop(data);
	}

	@Override
	public CastResult castAtLocation(SpellData data) {
		data = data.location(data.location().add(0, yOffset.get(data), 0));
		return initLoop(data);
	}

	@Override
	public CastResult castAtEntity(SpellData data) {
		return initLoop(data);
	}

	private CastResult initLoop(SpellData data) {
		Loop loop = new Loop(data);

		if (data.hasTarget()) activeLoops.put(data.target().getUniqueId(), loop);
		else if (data.hasCaster()) activeLoops.put(data.caster().getUniqueId(), loop);
		else activeLoops.put(null, loop);

		return new CastResult(PostCastAction.HANDLE_NORMALLY, data);
	}

	public Multimap<UUID, Loop> getActiveLoops() {
		return activeLoops;
	}

	public boolean isActive(LivingEntity entity) {
		return activeLoops.containsKey(entity.getUniqueId());
	}

	public void cancelLoops(LivingEntity entity) {
		Collection<Loop> loops = activeLoops.removeAll(entity.getUniqueId());
		loops.forEach(l -> l.cancel(false));
	}

	public void cancelLoops(UUID uuid) {
		Collection<Loop> loops = activeLoops.removeAll(uuid);
		loops.forEach(l -> l.cancel(false));
	}

	@Override
	protected void turnOff() {
		if (deathListener != null) {
			unregisterEvents(deathListener);
			deathListener = null;
		}

		activeLoops.forEach((target, loop) -> loop.cancel(false));
		activeLoops.clear();
	}

	public class Loop implements Runnable {

		private SpellData data;

		private final boolean stopOnFail;
		private final boolean cancelOnDeath;
		private final boolean passTargeting;
		private final boolean stopOnSuccess;
		private final boolean castRandomSpellInstead;
		private final boolean skipFirstLoopModifiers;
		private final boolean skipFirstVariableModsLoop;
		private final boolean skipFirstLoopTargetModifiers;
		private final boolean skipFirstLoopLocationModifiers;
		private final boolean skipFirstVariableModsTargetLoop;

		private final long iterations;
		private final int taskId;

		private long count;

		private Loop(SpellData data) {
			this.data = data;

			taskId = MagicSpells.scheduleRepeatingTask(this, delay.get(data), interval.get(data));
			iterations = LoopSpell.this.iterations.get(data);

			stopOnFail = LoopSpell.this.stopOnFail.get(data);
			cancelOnDeath = LoopSpell.this.cancelOnDeath.get(data);
			passTargeting = LoopSpell.this.passTargeting.get(data);
			stopOnSuccess = LoopSpell.this.stopOnSuccess.get(data);
			castRandomSpellInstead = LoopSpell.this.castRandomSpellInstead.get(data);
			skipFirstLoopModifiers = LoopSpell.this.skipFirstLoopModifiers.get(data);
			skipFirstVariableModsLoop = LoopSpell.this.skipFirstVariableModsLoop.get(data);
			skipFirstLoopTargetModifiers = LoopSpell.this.skipFirstLoopTargetModifiers.get(data);
			skipFirstLoopLocationModifiers = LoopSpell.this.skipFirstLoopLocationModifiers.get(data);
			skipFirstVariableModsTargetLoop = LoopSpell.this.skipFirstVariableModsTargetLoop.get(data);

			long dur = duration.get(data);
			if (dur > 0) MagicSpells.scheduleDelayedTask(this::cancel, dur);
		}

		@Override
		public void run() {
			if (data.hasTarget() && !data.target().isValid()) {
				cancel();
				return;
			}

			if (variableModsLoop != null && (!skipFirstVariableModsLoop || count > 0) && data.caster() instanceof Player playerCaster) {
				VariableManager variableManager = MagicSpells.getVariableManager();

				for (Map.Entry<String, VariableMod> entry : variableModsLoop.entries()) {
					VariableMod mod = entry.getValue();
					if (mod == null) continue;

					variableManager.processVariableMods(entry.getKey(), mod, playerCaster, data);
				}
			}

			if (variableModsTargetLoop != null && (!skipFirstVariableModsTargetLoop || count > 0) && data.target() instanceof Player playerTarget) {
				VariableManager variableManager = MagicSpells.getVariableManager();

				for (Map.Entry<String, VariableMod> entry : variableModsTargetLoop.entries()) {
					VariableMod mod = entry.getValue();
					if (mod == null) continue;

					variableManager.processVariableMods(entry.getKey(), mod, playerTarget, data);
				}
			}

			if (loopModifiers != null && (!skipFirstLoopModifiers || count > 0)) {
				ModifierResult result = loopModifiers.apply(data.caster(), data);
				data = result.data();

				if (!result.check()) {
					cancel();
					return;
				}
			}

			if (data.hasTarget() && loopTargetModifiers != null && (!skipFirstLoopTargetModifiers || count > 0)) {
				ModifierResult result = loopTargetModifiers.apply(data.caster(), data.target(), data);
				data = result.data();

				if (!result.check()) {
					cancel();
					return;
				}
			}

			if (data.hasLocation() && loopLocationModifiers != null && (!skipFirstLoopLocationModifiers || count > 0)) {
				ModifierResult result = loopLocationModifiers.apply(data.caster(), data.location(), data);
				data = result.data();

				if (!result.check()) {
					cancel();
					return;
				}
			}

			if (spells != null) {
				if (castRandomSpellInstead) {
					Subspell spell = spells.get(random.nextInt(spells.size()));
					if (!cast(spell)) return;
				} else {
					for (Subspell spell : spells)
						if (!cast(spell))
							return;
				}
			}

			playSpellEffects(data);

			count++;
			if (iterations > 0 && count >= iterations) cancel();
		}

		private boolean cast(Subspell spell) {
			boolean success = spell.subcast(data, passTargeting).action() != PostCastAction.ALREADY_HANDLED;
			if (stopOnSuccess && success || stopOnFail && !success) {
				cancel();
				return false;
			}

			return true;
		}

		public LoopSpell getSpell() {
			return LoopSpell.this;
		}

		public LivingEntity getCaster() {
			return data.caster();
		}

		private void cancel() {
			cancel(true);
		}

		private void cancel(boolean remove) {
			MagicSpells.cancelTask(taskId);

			if (remove) {
				UUID key = null;
				if (data.hasTarget()) key = data.target().getUniqueId();
				else if (data.hasCaster()) key = data.caster().getUniqueId();

				activeLoops.remove(key, this);
			}

			if (data.hasTarget()) playSpellEffects(EffectPosition.DELAYED, data.target(), data);
			else if (data.hasLocation()) playSpellEffects(EffectPosition.DELAYED, data.location(), data);
			else if (data.hasCaster()) playSpellEffects(EffectPosition.DELAYED, data.caster(), data);

			if (data.hasCaster() || data.hasTarget()) {
				String[] replacements = getReplacements(data);

				sendMessage(strFadeSelf, data.caster(), data, replacements);
				sendMessage(strFadeTarget, data.target(), data, replacements);
			}

			if (spellOnEnd != null) spellOnEnd.subcast(data);
		}

	}

	private static class DeathListener implements Listener {

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		public void onDeath(PlayerDeathEvent event) {
			UUID uuid = event.getEntity().getUniqueId();

			List<Spell> spells = MagicSpells.getSpellsOrdered();
			for (Spell spell : spells) {
				if (!(spell instanceof LoopSpell loopSpell)) continue;

				Collection<Loop> loops = loopSpell.getActiveLoops().get(uuid);
				loops.removeIf(loop -> {
					if (!loop.cancelOnDeath) return false;
					loop.cancel(false);
					return true;
				});
			}
		}

	}

}
