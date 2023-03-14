package com.nisovin.magicspells.util;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;

import com.nisovin.magicspells.Spell;

public class SpellFilter {

	private Set<String> allowedSpells = null;
	private Set<String> blacklistedSpells = null;
	private Set<String> allowedTags = null;
	private Set<String> disallowedTags = null;
	
	private final boolean defaultReturn;
	private boolean emptyFilter = false;
	
	public SpellFilter(List<String> allowedSpells, List<String> blacklistedSpells, List<String> allowedTags, List<String> disallowedTags) {
		
		// Initialize the collections
		if (allowedSpells != null && !allowedSpells.isEmpty()) this.allowedSpells = new HashSet<>(allowedSpells);
		if (blacklistedSpells != null && !blacklistedSpells.isEmpty()) this.blacklistedSpells = new HashSet<>(blacklistedSpells);
		if (allowedTags != null && !allowedTags.isEmpty()) this.allowedTags = new HashSet<>(allowedTags);
		if (disallowedTags != null && !disallowedTags.isEmpty()) this.disallowedTags = new HashSet<>(disallowedTags);

		// Determine the default outcome if nothing catches
		defaultReturn = determineDefaultValue();
	}
	
	private boolean determineDefaultValue() {
		// This means there is a tag whitelist check
		if (allowedTags != null) return false;
		
		// If there is a spell whitelist check
		if (allowedSpells != null) return false;
		
		// This means there is a tag blacklist
		if (disallowedTags != null) return true;
		
		// If there is a spell blacklist
		if (blacklistedSpells != null) return true;
		
		// If all the collections are null, then there is no filter
		emptyFilter = true;
		return true;
	}
	
	public boolean check(Spell spell) {
		// Can't do anything if null anyway
		if (spell == null) return false;
		
		// Quick check to exit early if possible
		if (emptyFilter) return true;
		
		// Is it whitelisted explicitly?
		if (allowedSpells != null && allowedSpells.contains(spell.getInternalName())) return true;
		
		// Is it blacklisted?
		if (blacklistedSpells != null && blacklistedSpells.contains(spell.getInternalName())) return false;
		
		// Does it have a blacklisted tag?
		if (disallowedTags != null) {
			for (String tag : disallowedTags) {
				if (spell.hasTag(tag)) return false;
			}
		}
		
		// Does it have a whitelisted tag?
		if (allowedTags != null) {
			for (String tag : allowedTags) {
				if (spell.hasTag(tag)) return true;
			}
		}
		
		return defaultReturn;
	}
	
	public static SpellFilter fromConfig(MagicConfig config, String basePath) {
		basePath = basePath +  '.';
		List<String> spells = config.getStringList(basePath + "spells", null);
		List<String> deniedSpells = config.getStringList(basePath + "denied-spells", null);
		List<String> tagList = config.getStringList(basePath + "spell-tags", null);
		List<String> deniedTagList = config.getStringList(basePath + "denied-spell-tags", null);
		return new SpellFilter(spells, deniedSpells, tagList, deniedTagList);
	}

	/**
	 * Create a {@link SpellFilter} instance out of a formatted string.
	 * @param string Follows format: allowedSpell, !disallowedSpell, tag:allowedTag, !tag:disallowedTag
	 */
	public static SpellFilter fromString(String string) {
		List<String> spells = new ArrayList<>();
		List<String> deniedSpells = new ArrayList<>();
		List<String> spellTags = new ArrayList<>();
		List<String> deniedSpellTags = new ArrayList<>();

		String[] split = string.split(",");
		for (String s : split) {
			boolean denied = false;
			s = s.trim();

			if (s.startsWith("!")) {
				s = s.substring(1);
				denied = true;
			}

			if (s.toLowerCase().startsWith("tag:")) {
				s = s.substring(4);
				if (denied) deniedSpellTags.add(s);
				else spellTags.add(s);
			} else {
				if (denied) deniedSpells.add(s);
				else spells.add(s);
			}
		}

		return new SpellFilter(spells, deniedSpells, spellTags, deniedSpellTags);
	}

}
