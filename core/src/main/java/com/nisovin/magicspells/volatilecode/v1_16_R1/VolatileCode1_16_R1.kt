package com.nisovin.magicspells.volatilecode.v1_16_R1

import java.util.UUID
import java.lang.reflect.Field

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.*
import org.bukkit.inventory.*
import org.bukkit.util.Vector
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.craftbukkit.v1_16_R1.entity.*
import org.bukkit.persistence.PersistentDataType
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.craftbukkit.v1_16_R1.CraftServer
import org.bukkit.craftbukkit.v1_16_R1.inventory.CraftItemStack

import com.nisovin.magicspells.util.*
import com.nisovin.magicspells.MagicSpells
import com.nisovin.magicspells.util.ColorUtil
import com.nisovin.magicspells.util.compat.EventUtil
import com.nisovin.magicspells.volatilecode.VolatileCodeHandle

import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent

import net.minecraft.server.v1_16_R1.*

private typealias nmsItemStack = net.minecraft.server.v1_16_R1.ItemStack

class VolatileCode1_16_R1: VolatileCodeHandle {

    private var entityFallingBlockFallHurtAmountField: Field? = null
    private var entityFallingBlockFallHurtMaxField: Field? = null
    private var craftMetaSkullClass: Class<*>? = null
    private var craftMetaSkullProfileField: Field? = null

    init {
        try {
            this.entityFallingBlockFallHurtAmountField = EntityFallingBlock::class.java.getDeclaredField("fallHurtAmount")
            this.entityFallingBlockFallHurtAmountField!!.isAccessible = true

            this.entityFallingBlockFallHurtMaxField = EntityFallingBlock::class.java.getDeclaredField("fallHurtMax")
            this.entityFallingBlockFallHurtMaxField!!.isAccessible = true

            this.craftMetaSkullClass = Class.forName("org.bukkit.craftbukkit.v1_16_R1.inventory.CraftMetaSkull")
            this.craftMetaSkullProfileField = this.craftMetaSkullClass!!.getDeclaredField("profile")
            this.craftMetaSkullProfileField!!.isAccessible = true
        } catch (e: Exception) {
            MagicSpells.error("THIS OCCURRED WHEN CREATING THE VOLATILE CODE HANDLE FOR 1.16.1, THE FOLLOWING ERROR IS MOST LIKELY USEFUL IF YOU'RE RUNNING THE LATEST VERSION OF MAGICSPELLS.")
            e.printStackTrace()
        }
    }

    override fun addPotionGraphicalEffect(entity: LivingEntity, color: Int, duration: Int) {
        /*final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
        final DataWatcher dw = el.getDataWatcher();
        dw.watch(7, Integer.valueOf(color));
        if (duration > 0) {
            MagicSpells.scheduleDelayedTask(new Runnable() {
                public void run() {
                    int c = 0;
                    if (!el.effects.isEmpty()) {
                        c = net.minecraft.server.v1_12_R1.PotionBrewer.a(el.effects.values());
                    }
                    dw.watch(7, Integer.valueOf(c));
                }
            }, duration);
        }*/
    }

    override fun creaturePathToLoc(creature: Creature, loc: Location, speed: Float) {
        val entity = (creature as CraftCreature).handle
        val pathEntity = entity.navigation.a(loc.x, loc.y, loc.z, 1)
        entity.navigation.a(pathEntity, speed.toDouble())
    }

    override fun sendFakeSlotUpdate(player: Player, slot: Int, item: ItemStack?) {
        val nmsItem: nmsItemStack?
        if (item != null) {
            nmsItem = CraftItemStack.asNMSCopy(item)
        } else {
            nmsItem = null
        }
        val packet = PacketPlayOutSetSlot(0, slot.toShort() + 36, nmsItem!!)
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }

    override fun simulateTnt(target: Location, source: LivingEntity, explosionSize: Float, fire: Boolean): Boolean {
        val e = EntityTNTPrimed((target.world as CraftWorld).handle, target.x, target.y, target.z, (source as CraftLivingEntity).handle)
        val c = CraftTNTPrimed(Bukkit.getServer() as CraftServer, e)
        val event = ExplosionPrimeEvent(c, explosionSize, fire)
        EventUtil.call(event)
        return event.isCancelled
    }

    override fun createExplosionByEntity(entity: Entity, location: Location, size: Float, fire: Boolean, breakBlocks: Boolean): Boolean {
        return location.world!!.createExplosion(location, size, fire, breakBlocks, entity)
    }

    override fun setExperienceBar(player: Player, level: Int, percent: Float) {
        player.sendExperienceChange(percent, level)
    }

    override fun setTarget(entity: LivingEntity?, target: LivingEntity?) {
        if (entity is Mob) {
            entity.target = target
        }
    }

    override fun setFallingBlockHurtEntities(block: FallingBlock, damage: Float, max: Int) {
        val efb = (block as CraftFallingBlock).handle
        try {
            block.setHurtEntities(true)
            this.entityFallingBlockFallHurtAmountField!!.setFloat(efb, damage)
            this.entityFallingBlockFallHurtMaxField!!.setInt(efb, max)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun playDragonDeathEffect(location: Location) {
        val dragon = EntityEnderDragon(EntityTypes.ENDER_DRAGON, (location.world as CraftWorld).handle)
        dragon.setPositionRotation(location.x, location.y, location.z, location.yaw, 0f)

        val packet24 = PacketPlayOutSpawnEntityLiving(dragon)
        val packet38 = PacketPlayOutEntityStatus(dragon, 3.toByte())
        val packet29 = PacketPlayOutEntityDestroy(dragon.bukkitEntity.entityId)

        val box = BoundingBox(location, 64.0)
        val players = ArrayList<Player>()
        for (player in location.world!!.players) {
            if (!box.contains(player)) continue
            players.add(player)
            (player as CraftPlayer).handle.playerConnection.sendPacket(packet24)
            player.handle.playerConnection.sendPacket(packet38)
        }

        MagicSpells.scheduleDelayedTask({
            for (player in players) {
                if (player.isValid) {
                    (player as CraftPlayer).handle.playerConnection.sendPacket(packet29)
                }
            }
        }, 250)
    }

    override fun setKiller(entity: LivingEntity, killer: Player) {
        (entity as CraftLivingEntity).handle.killer = (killer as CraftPlayer).handle
    }

    override fun addAILookAtPlayer(entity: LivingEntity, range: Int) {
        try {
            val ev = (entity as CraftLivingEntity).handle as EntityInsentient

            val goalsField = EntityInsentient::class.java.getDeclaredField("goalSelector")
            goalsField.isAccessible = true
            val goals = goalsField.get(ev) as PathfinderGoalSelector

            goals.a(1, PathfinderGoalLookAtPlayer(ev, EntityHuman::class.java, range.toFloat(), 1.0f))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun setClientVelocity(player: Player, velocity: Vector) {
        val packet = PacketPlayOutEntityVelocity(player.entityId, Vec3D(velocity.x, velocity.y, velocity.z))
        (player as CraftPlayer).handle.playerConnection.sendPacket(packet)
    }

    override fun getAbsorptionHearts(entity: LivingEntity): Double {
        return entity.absorptionAmount
    }

    override fun setAbsorptionHearts(entity: LivingEntity, amount: Double) {
        entity.absorptionAmount = amount
    }

    override fun setTexture(meta: SkullMeta, texture: String, signature: String) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftMetaSkullProfileField)) return

        try {
            val profile = this.craftMetaSkullProfileField!!.get(meta) as GameProfile
            setTexture(profile, texture, signature)
            this.craftMetaSkullProfileField!!.set(meta, profile)
        } catch (e: SecurityException) {
            MagicSpells.handleException(e)
        } catch (e: IllegalArgumentException) {
            MagicSpells.handleException(e)
        } catch (e: IllegalAccessException) {
            MagicSpells.handleException(e)
        }

    }

    override fun getSkinData(player: Player): String {
        val latestSkin = (player as CraftPlayer).handle.profile.properties.get("textures").first()
        return "Skin: " + latestSkin.value + "\nSignature: " + latestSkin.signature
    }

    override fun setSkin(player: Player, skin: String, signature: String) {
        val craftPlayer = player as CraftPlayer
        setTexture(craftPlayer.profile, skin, signature)
    }

    override fun colorize(message: String?): String? {
        val matcher = ColorUtil.HEX_PATTERN.matcher(org.bukkit.ChatColor.translateAlternateColorCodes('&', message!!))
        val buffer = StringBuffer()
        while (matcher.find()) {
            try {
                matcher.appendReplacement(buffer, ChatColor.of(matcher.group(1).toUpperCase()).toString())
            } catch (exception: IllegalArgumentException) {
                // ignored
            }
        }
        return matcher.appendTail(buffer).toString()
    }

    private fun setTexture(profile: GameProfile, texture: String, signature: String?): GameProfile {
        if (signature == null || signature.isEmpty()) {
            profile.properties.put("textures", Property("textures", texture))
        } else {
            profile.properties.put("textures", Property("textures", texture, signature))
        }
        return profile
    }

    override fun setTexture(meta: SkullMeta, texture: String, signature: String, uuid: String?, offlinePlayer: OfflinePlayer) {
        // Don't spam the user with errors, just stop
        if (SafetyCheckUtils.areAnyNull(this.craftMetaSkullProfileField)) return

        try {
            val profile = GameProfile(if (uuid != null) UUID.fromString(uuid) else null, offlinePlayer.name)
            setTexture(profile, texture, signature)
            this.craftMetaSkullProfileField!!.set(meta, profile)
        } catch (e: SecurityException) {
            MagicSpells.handleException(e)
        } catch (e: IllegalArgumentException) {
            MagicSpells.handleException(e)
        } catch (e: IllegalAccessException) {
            MagicSpells.handleException(e)
        }

    }

    override fun getCustomModelData(meta: ItemMeta?): Int {
        if (meta == null) return 0
        if (meta.hasCustomModelData()) return meta.customModelData
        return 0
    }

    override fun setCustomModelData(meta: ItemMeta?, data: Int) {
        meta?.setCustomModelData(data)
    }

    override fun setNBTString(item: ItemStack, key: String, value: String): ItemStack {
        val meta = if (item.hasItemMeta()) item.itemMeta else Bukkit.getItemFactory().getItemMeta(item.type)
        meta?.persistentDataContainer?.set(NamespacedKey(MagicSpells.plugin, key), PersistentDataType.STRING, value)
        item.itemMeta = meta
        return item
    }

    override fun getNBTString(item: ItemStack, key: String): String? {
        return item.itemMeta?.persistentDataContainer?.get(NamespacedKey(MagicSpells.plugin, key), PersistentDataType.STRING)
    }

    override fun setInventoryTitle(player: Player, title: String) {
        val entityPlayer = (player as CraftPlayer).handle
        val container = entityPlayer.activeContainer
        val packet = PacketPlayOutOpenWindow(container.windowId, container.type, ChatMessage(title))
        entityPlayer.playerConnection.sendPacket(packet)
        entityPlayer.updateInventory(container)
    }

    override fun createCookingRecipe(type: String, namespaceKey: NamespacedKey, group: String, result: ItemStack, ingredient: Material, experience: Float, cookingTime: Int): Recipe {
        var recipe : Recipe? = null
        when (type) {
            "smoking" -> recipe = SmokingRecipe(namespaceKey, result, ingredient, experience, cookingTime)
            "campfire" -> recipe = CampfireRecipe(namespaceKey, result, ingredient, experience, cookingTime)
            "blasting" -> recipe = BlastingRecipe(namespaceKey, result, ingredient, experience, cookingTime)
        }
        (recipe as CookingRecipe<*>).group = group
        return recipe
    }

    override fun createStonecutterRecipe(namespaceKey: NamespacedKey, group: String, result: ItemStack, ingredient: Material): Recipe {
        val recipe = StonecuttingRecipe(namespaceKey, result, ingredient)
        recipe.group = group
        return recipe
    }

    override fun createSmithingRecipe(namespaceKey: NamespacedKey, result: ItemStack, base: Material, addition: Material): Recipe {
        return SmithingRecipe(namespaceKey, result, RecipeChoice.MaterialChoice(base), RecipeChoice.MaterialChoice(addition))
    }

    override fun sendActionBarMessage(player: Player, message: String?) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message))
    }

}
