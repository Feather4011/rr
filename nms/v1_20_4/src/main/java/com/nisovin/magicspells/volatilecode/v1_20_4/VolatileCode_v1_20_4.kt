package com.nisovin.magicspells.volatilecode.v1_20_4

import java.util.*

import org.bukkit.Bukkit
import org.bukkit.entity.*
import org.bukkit.Location
import org.bukkit.util.Vector
import org.bukkit.NamespacedKey
import org.bukkit.inventory.Recipe
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import org.bukkit.event.entity.ExplosionPrimeEvent
import org.bukkit.inventory.SmithingTransformRecipe

import org.bukkit.craftbukkit.v1_20_R3.entity.*
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import org.bukkit.craftbukkit.v1_20_R3.CraftServer
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack

import net.kyori.adventure.text.Component

import io.papermc.paper.util.MCUtil
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.advancement.AdvancementDisplay

import net.minecraft.advancements.*
import net.minecraft.world.phys.Vec3
import net.minecraft.world.entity.EntityType
import net.minecraft.network.protocol.game.*
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.item.alchemy.PotionUtils
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.advancements.critereon.ImpossibleTrigger
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload

import com.nisovin.magicspells.volatilecode.VolatileCodeHandle
import com.nisovin.magicspells.volatilecode.VolatileCodeHelper

class VolatileCode_v1_20_4(helper: VolatileCodeHelper) : VolatileCodeHandle(helper) {

    private val toastKey = ResourceLocation("magicspells", "toast_effect")

    private var entityLivingPotionEffectColor: EntityDataAccessor<Int>? = null

    init {
        try {
            // CHANGE THIS TO SPIGOT MAPPING VERSION OF MOJANG'S - EntityDataAccessor<Integer> DATA_EFFECT_COLOR_ID
            val entityLivingPotionEffectColorField = net.minecraft.world.entity.LivingEntity::class.java.getDeclaredField("bI")
            entityLivingPotionEffectColorField.isAccessible = true
            entityLivingPotionEffectColor = entityLivingPotionEffectColorField.get(null) as EntityDataAccessor<Int>
        } catch (e: Exception) {
            helper.error("THIS OCCURRED WHEN CREATING THE VOLATILE CODE HANDLE FOR 1.20.4, THE FOLLOWING ERROR IS MOST LIKELY USEFUL IF YOU'RE RUNNING THE LATEST VERSION OF MAGICSPELLS.")
            e.printStackTrace()
        }
    }

    override fun addPotionGraphicalEffect(entity: LivingEntity, color: Int, duration: Long) {
        val livingEntity = (entity as CraftLivingEntity).handle
        val entityData = livingEntity.entityData
        entityData.set(entityLivingPotionEffectColor, color)

        if (duration > 0) {
            helper.scheduleDelayedTask({
                var c = 0
                if (livingEntity.getActiveEffects().isNotEmpty()) {
                    c = PotionUtils.getColor(livingEntity.getActiveEffects())
                }
                entityData.set(entityLivingPotionEffectColor, c)
            }, duration)
        }
    }

    override fun sendFakeSlotUpdate(player: Player, slot: Int, item: ItemStack?) {
        val nmsItem = CraftItemStack.asNMSCopy(item)
        val packet = ClientboundContainerSetSlotPacket(0, 0, slot.toShort() + 36, nmsItem)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun simulateTnt(target: Location, source: LivingEntity, explosionSize: Float, fire: Boolean): Boolean {
        val e = PrimedTnt((target.world as CraftWorld).handle, target.x, target.y, target.z, (source as CraftLivingEntity).handle)
        val c = CraftTNTPrimed(Bukkit.getServer() as CraftServer, e)
        val event = ExplosionPrimeEvent(c, explosionSize, fire)
        Bukkit.getPluginManager().callEvent(event)
        return event.isCancelled
    }

    override fun playDragonDeathEffect(location: Location) {
        val dragon = EnderDragon(EntityType.ENDER_DRAGON, (location.world as CraftWorld).handle)
        dragon.setPos(location.x, location.y, location.z)

        val addMobPacket = ClientboundAddEntityPacket(dragon)
        val entityEventPacket = ClientboundEntityEventPacket(dragon, 3)
        val removeEntityPacket = ClientboundRemoveEntitiesPacket(dragon.id)

        val players = ArrayList<Player>()
        for (player in location.getNearbyPlayers(64.0)) {
            players.add(player)
            (player as CraftPlayer).handle.connection.send(addMobPacket)
            player.handle.connection.send(addMobPacket)
            player.handle.connection.send(entityEventPacket)
        }

        helper.scheduleDelayedTask({
            for (player in players) {
                if (!player.isValid) continue
                (player as CraftPlayer).handle.connection.send(removeEntityPacket)
            }
        }, 250)

    }

    override fun setClientVelocity(player: Player, velocity: Vector) {
        val packet = ClientboundSetEntityMotionPacket(player.entityId, Vec3(velocity.x, velocity.y, velocity.z))
        (player as CraftPlayer).handle.connection.send(packet)
    }

    override fun startAutoSpinAttack(player: Player?, ticks: Int) {
        val entityPlayer = (player as CraftPlayer).handle
        entityPlayer.startAutoSpinAttack(ticks)
    }

    override fun playHurtAnimation(entity: LivingEntity, yaw: Float) {
        val e = (entity as CraftLivingEntity).handle

        for (p : Player in entity.location.getNearbyPlayers((entity.server.simulationDistance * 16).toDouble())) {
            (p as CraftPlayer).handle.connection.send(ClientboundHurtAnimationPacket(e.id, 90 + yaw))
        }

        if (e.isSilent) return
        val sound = e.getHurtSound0(e.damageSources().generic())
        e.level().playSound(null, e.x, e.y, e.z, sound, e.soundSource, e.soundVolume, e.voicePitch)
    }

    override fun createSmithingRecipe(
        namespacedKey: NamespacedKey,
        result: ItemStack,
        template: RecipeChoice,
        base: RecipeChoice,
        addition: RecipeChoice,
        copyNbt: Boolean
    ): Recipe {
        return SmithingTransformRecipe(namespacedKey, result, template, base, addition, copyNbt)
    }

    override fun sendToastEffect(receiver: Player, icon: ItemStack, frameType: AdvancementDisplay.Frame, text: Component) {
        val iconNms = CraftItemStack.asNMSCopy(icon)
        val textNms = PaperAdventure.asVanilla(text)
        val description = PaperAdventure.asVanilla(Component.empty())
        val frame = try {
            AdvancementType.valueOf(frameType.name)
        } catch (_: IllegalArgumentException) {
            AdvancementType.TASK
        }

        val advancement = Advancement.Builder.advancement()
            .display(iconNms, textNms, description, null, frame, true, false, true)
            .addCriterion("impossible", Criterion(ImpossibleTrigger(), ImpossibleTrigger.TriggerInstance()))
            .build(toastKey)
        val progress = AdvancementProgress()
        progress.update(AdvancementRequirements(listOf(listOf("impossible"))))
        progress.grantProgress("impossible")

        val player = (receiver as CraftPlayer).handle
        player.connection.send(ClientboundUpdateAdvancementsPacket(
            false,
            Collections.singleton(advancement),
            Collections.emptySet(),
            Collections.singletonMap(toastKey, progress)
        ))
        player.connection.send(ClientboundUpdateAdvancementsPacket(
            false,
            Collections.emptySet(),
            Collections.singleton(toastKey),
            Collections.emptyMap()
        ))
    }

    override fun sendStatusUpdate(player: Player, health: Double, food: Int, saturation: Float) {
        var displayedHealth = health
        if (player.isHealthScaled) {
            displayedHealth = player.health / player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value * player.healthScale
        }

        (player as CraftPlayer).handle.connection.send(ClientboundSetHealthPacket(displayedHealth.toFloat(), food, saturation))
    }

    override fun addGameTestMarker(player: Player, location: Location, color: Int, name: String, lifetime: Int) {
        val payload = GameTestAddMarkerDebugPayload(MCUtil.toBlockPosition(location), color, name, lifetime)
        (player as CraftPlayer).handle.connection.send(ClientboundCustomPayloadPacket(payload))
    }

    override fun clearGameTestMarkers(player: Player) {
        val payload = GameTestClearMarkersDebugPayload()
        (player as CraftPlayer).handle.connection.send(ClientboundCustomPayloadPacket(payload))
    }

}
