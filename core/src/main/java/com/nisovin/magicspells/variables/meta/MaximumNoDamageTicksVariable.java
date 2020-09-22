package com.nisovin.magicspells.variables.meta;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.PlayerNameUtils;
import com.nisovin.magicspells.variables.variabletypes.MetaVariable;

public class MaximumNoDamageTicksVariable extends MetaVariable {

	@Override
	public double getValue(String player) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) return p.getMaximumNoDamageTicks();
		return 0;
	}
	
	@Override
	public void set(String player, double amount) {
		Player p = PlayerNameUtils.getPlayerExact(player);
		if (p != null) p.setMaximumNoDamageTicks((int) amount);
	}

}
