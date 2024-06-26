package deadlydisasters.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import deadlydisasters.disasters.BlackPlague;
import deadlydisasters.disasters.EndStorm;
import deadlydisasters.entities.CustomEntity;
import deadlydisasters.entities.christmasentities.ElfPet;
import deadlydisasters.entities.endstormentities.BabyEndTotem;
import deadlydisasters.entities.endstormentities.EndWorm;
import deadlydisasters.entities.purgeentities.DarkMage;
import deadlydisasters.entities.soulstormentities.SoulReaper;
import deadlydisasters.general.ItemsHandler;
import deadlydisasters.general.Languages;
import deadlydisasters.general.Main;
import deadlydisasters.utils.Utils;

public class CustomEntitiesListener implements Listener {
	
	private Main plugin;
	
	public CustomEntitiesListener(Main plugin) {
		this.plugin = plugin;
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	@EventHandler
	public void onAttack(EntityDamageByEntityEvent e) {
		Entity damager = e.getDamager();
		if (damager instanceof Arrow && damager.hasMetadata("dd-voidarrow")) {
			if (!Utils.isZoneProtected(damager.getLocation()) || plugin.getConfig().getBoolean("customitems.items.void_wrath.allow_in_regions"))
				EndStorm.createUnstableRift(e.getEntity().getLocation(), ItemsHandler.voidBowPortalTicks);
			damager.remove();
			return;
		}
		if (damager instanceof Projectile && e.getEntity().hasMetadata("dd-frosty")) {
			e.setCancelled(true);
			return;
		}
		if (damager instanceof EvokerFangs && e.getEntity() instanceof Player && ((Player) e.getEntity()).getHealth() <= e.getFinalDamage() && damager.hasMetadata("dd-endworm")) {
			e.getEntity().setMetadata("dd-endwormfangs", plugin.fixedData);
			return;
		}
		if (damager instanceof ShulkerBullet && damager.hasMetadata("dd-magebullet") && e.getEntity() instanceof LivingEntity) {
			LivingEntity temp = (LivingEntity) e.getEntity();
			temp.setVelocity(damager.getVelocity().multiply(3).setY(1));
			temp.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true));
			temp.removePotionEffect(PotionEffectType.LEVITATION);
			plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
				public void run() {
					temp.removePotionEffect(PotionEffectType.LEVITATION);
				}
			}, 1);
		}
		if (!(damager instanceof LivingEntity)) return;
		LivingEntity entity = (LivingEntity) damager;
		if (e.getEntity().hasMetadata("dd-plague") && !damager.hasMetadata("dd-plague") && BlackPlague.time.size() < BlackPlague.maxInfectedMobs) {
			if (entity instanceof Player) {
				if (!Utils.isPlayerImmune((Player) entity)) {
					entity.sendMessage(Utils.chat("&c"+Languages.langFile.getString("misc.plagueCatch")));
					BlackPlague.time.put(entity.getUniqueId(), 300);
					entity.setMetadata("dd-plague", plugin.fixedData);
				}
			} else {
				BlackPlague.time.put(entity.getUniqueId(), 300);
				entity.setMetadata("dd-plague", plugin.fixedData);
			}
		}
		if (entity.hasMetadata("dd-customentity")) {
			if (entity.hasMetadata("dd-endtotem")) {
				Location loc = e.getEntity().getLocation();
				e.getEntity().teleport(loc.add(0, 0.15, 0));
				if (plugin.mcVersion >= 1.16)
					e.getEntity().setVelocity(new Vector(loc.getX() - entity.getLocation().getX(), 0.1, loc.getZ() - entity.getLocation().getZ()).normalize().multiply(entity.getAttribute(Attribute.GENERIC_ATTACK_KNOCKBACK).getBaseValue()));
				if (entity instanceof Enderman)
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, 1f, 2f);
				else
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.HOSTILE, .3f, 2f);
				if (!entity.hasMetadata("dd-animation"))
					entity.setMetadata("dd-animation", plugin.fixedData);
			} else if (entity.hasMetadata("dd-voidguardian"))
				e.setDamage(Math.max(18 - (entity.getHealth() / 4), 4));
			else if (entity.hasMetadata("dd-voidstalker") && e.getEntity() instanceof LivingEntity)
				((LivingEntity) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true));
			else if (entity.hasMetadata("dd-ancientmummy") && e.getEntity() instanceof LivingEntity)
				((LivingEntity) e.getEntity()).addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 4, true));
			else if (entity.hasMetadata("dd-endworm"))
				((EndWorm) plugin.handler.findEntity(entity)).triggerAnimation();
			else if (entity.hasMetadata("dd-petelf") && ((ElfPet) plugin.handler.findEntity((LivingEntity) entity)).owner.equals(e.getEntity().getUniqueId()))
				e.setCancelled(true);
			
			if (e.getEntity() instanceof Player && e.getFinalDamage() >= ((LivingEntity) e.getEntity()).getHealth()) {
				if (entity.hasMetadata("dd-sandstormmob"))
					e.getEntity().setMetadata("dd-sandstormdeath", plugin.fixedData);
				else if (entity.hasMetadata("dd-purgemob"))
					e.getEntity().setMetadata("dd-purgedeath", plugin.fixedData);
				else if (entity.hasMetadata("dd-lostsoul")) {
					entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_VEX_CHARGE, SoundCategory.HOSTILE, 2f, .5f);
					entity.remove();
					e.getEntity().setMetadata("dd-lostsouldeath", plugin.fixedData);
				}
			}
		}
		if (e.getEntity().hasMetadata("dd-darkmage") && !e.getEntity().isDead()) {
			LivingEntity damaged = (LivingEntity) e.getEntity();
			if (damaged.getHealth() < 12 && e.getFinalDamage() < damaged.getHealth())
				((DarkMage) plugin.handler.findEntity(damaged)).reboundTarget = entity;
		} else if (e.getEntity().hasMetadata("dd-soulreaper") && !e.getEntity().isDead()) {
			((SoulReaper) plugin.handler.findEntity((LivingEntity) e.getEntity())).target = entity;
		}
	}
	@EventHandler
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity().hasMetadata("dd-invulnerable")) {
			e.setCancelled(true);
			return;
		}
		if (!e.getEntity().hasMetadata("dd-customentity"))
			return;
		Entity entity = e.getEntity();
		if (entity.hasMetadata("dd-soulreaper") && e.getCause() != DamageCause.ENTITY_ATTACK)
			e.setCancelled(true);
		else if (e.getCause() == DamageCause.FALL && entity.hasMetadata("dd-eastermobs"))
			e.setCancelled(true);
		else if (e.getCause() == DamageCause.ENTITY_EXPLOSION && entity.hasMetadata("dd-easterbunny"))
			e.setCancelled(true);
		else if (e.getCause() == DamageCause.SUFFOCATION && entity.hasMetadata("dd-killerchickenghost"))
			e.setCancelled(true);
	}
	@EventHandler
	public void onDeath(EntityDeathEvent e) {
		if (e.getEntity().hasMetadata("dd-customentity"))
			for (ItemStack item : e.getDrops())
				item.setType(Material.AIR);
	}
	@EventHandler
	public void onEntityInteract(PlayerInteractEntityEvent e) {
		if (!e.getRightClicked().hasMetadata("dd-customentity")) return;
		LivingEntity entity = (LivingEntity) e.getRightClicked();
		if (entity.hasMetadata("dd-endtotem") && entity instanceof Wolf) {
			ItemStack item = null;
			if (plugin.mcVersion >= 1.16)
				item = e.getPlayer().getInventory().getItem(e.getHand());
			else
				item = e.getPlayer().getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) return;
			if (item.getType() == Material.CHORUS_FRUIT && entity.getHealth() < entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
				if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
					item.setAmount(item.getAmount()-1);
				entity.setHealth(Math.min(entity.getHealth()+4, entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
				entity.getWorld().spawnParticle(Particle.COMPOSTER, entity.getLocation().clone().add(0,.5,0), 12, .25, .25, .25, .01);
			} else if (item.getType() == Material.GHAST_TEAR && ((Wolf) entity).getOwner() == null) {
				if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
					item.setAmount(item.getAmount()-1);
				if (plugin.random.nextInt(4) == 0) {
					entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().clone().add(0,.5,0), 7, .25, .3, .25, .03);
					((Wolf) entity).setOwner(e.getPlayer());
					entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_CONDUIT_ATTACK_TARGET, SoundCategory.HOSTILE, .5f, .5f);
				}
			} else if (item.getType() == Material.NAME_TAG && (((Wolf) entity).getOwner() != null && ((Wolf) entity).getOwner().equals(e.getPlayer()))) {
				if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					((BabyEndTotem) plugin.handler.findEntity(entity)).changeName(item.getItemMeta().getDisplayName());
					if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
						item.setAmount(item.getAmount()-1);
				}
			}
			e.setCancelled(true);
		}
	}
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent e) {
		if (e.isCancelled())
			return;
		if (e.getHitEntity() instanceof LivingEntity && e.getEntity().getShooter() instanceof Snowman && ((Snowman) e.getEntity().getShooter()).hasMetadata("dd-frosty")) {
			if (plugin.mcVersion >= 1.17)
				e.getHitEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.HOSTILE, 2, .5f);
			if (e.getHitEntity() instanceof Player && ((Player) e.getHitEntity()).isBlocking()) {
				e.setCancelled(true);
				((Player) e.getHitEntity()).playSound(e.getHitEntity(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1, 1);
				return;
			}
			((LivingEntity) e.getHitEntity()).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
			Utils.damageEntity((LivingEntity) e.getHitEntity(), 8.0, "dd-frostydeath", false);
		}
		if (e.getEntity().hasMetadata("dd-elfarrow")) {
			e.getEntity().getWorld().createExplosion(e.getEntity().getLocation(), 1.5f, false, false, e.getEntity());
			e.getEntity().remove();
			if (e.getHitEntity() instanceof LivingEntity)
				((LivingEntity) e.getHitEntity()).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
		}
		if (e.getEntity().hasMetadata("dd-petelfarrow")) {
			e.getEntity().getWorld().createExplosion(e.getEntity().getLocation(), 1.5f, false, false, e.getEntity());
			e.getEntity().remove();
			if (e.getHitEntity() instanceof LivingEntity)
				((LivingEntity) e.getHitEntity()).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, true, false));
		}
	}
	@EventHandler(priority = EventPriority.HIGH)
	public void onTarget(EntityTargetLivingEntityEvent e) {
		if (e.getTarget() == null || e.isCancelled())
			return;
		Entity entity = e.getEntity();
		if (entity.hasMetadata("dd-christmasmob") && e.getTarget().hasMetadata("dd-christmasmob")) {
			e.setCancelled(true);
			return;
		}
		if (entity.hasMetadata("dd-killerchicken") && e.getTarget().hasMetadata("dd-easterbunny")) {
			e.setCancelled(true);
			return;
		}
		if (entity.hasMetadata("dd-petelf")) {
			CustomEntity ce = plugin.handler.findEntity((LivingEntity) entity);
			if (ce != null) {
				if (((ElfPet) ce).owner.equals(e.getTarget().getUniqueId()) ||
						(e.getTarget().hasMetadata("dd-petelf") && plugin.handler.findEntity(e.getTarget()) != null && ((ElfPet) plugin.handler.findEntity(e.getTarget())).owner.equals(((ElfPet) ce).owner))) {
					e.setCancelled(true);
					return;
				}
				if (e.getReason() == TargetReason.TARGET_ATTACKED_ENTITY) {
					((ElfPet) ce).target = e.getTarget();
					if (CustomEnchantHandler.santaHatPlayers.containsKey(((ElfPet) ce).owner))
						for (ElfPet elf : CustomEnchantHandler.santaHatPlayers.get(((ElfPet) ce).owner))
							if (elf != null && elf.getEntity() != null && !elf.getEntity().isDead() && (elf.target == null || elf.target.isDead()))
								elf.target = e.getTarget();
				}
			}
			return;
		}
	}
}
