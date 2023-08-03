package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.handlers.DebugHandler;
import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;

public class VariableCondition extends OperatorCondition {

	private static final Pattern VARIABLE_NAME_PATTERN = Pattern.compile("[0-9a-zA-Z_]+");

	private String variable;
	private String variableCompared;
	private double value = 0;

	@Override
	public boolean initialize(String var) {
		Matcher matcher = VARIABLE_NAME_PATTERN.matcher(var);
		String variableName;

		if (matcher.find()) variableName = matcher.group();
		else return false;

		String number = var.substring(variableName.length());
		if (number.length() < 2 || !super.initialize(number)) return false;

		variable = variableName;

		try {
			value = Double.parseDouble(number.substring(1));
			return true;
		} catch (NumberFormatException e) {
			variableCompared = number.substring(1);
			if (MagicSpells.getVariableManager().getVariable(variableCompared) != null) return true;
			else variableCompared = null;

			DebugHandler.debugNumberFormat(e);
			return false;
		}
	}

	@Override
	public boolean check(LivingEntity caster) {
		return variable(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return variable(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return variable(caster);
	}

	private boolean variable(LivingEntity target) {
		if (!(target instanceof Player pl)) return false;
		double with = variableCompared == null ? value : MagicSpells.getVariableManager().getValue(variableCompared, pl);
		return compare(MagicSpells.getVariableManager().getValue(variable, pl), with);
	}

}
