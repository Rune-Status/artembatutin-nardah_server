package io.battlerune.game.world.entity.combat.strategy.npc;

import io.battlerune.game.Animation;
import io.battlerune.game.Graphic;
import io.battlerune.game.UpdatePriority;
import io.battlerune.game.world.entity.combat.CombatImpact;
import io.battlerune.game.world.entity.combat.CombatType;
import io.battlerune.game.world.entity.combat.attack.FightType;
import io.battlerune.game.world.entity.combat.effect.impl.CombatPoisonEffect;
import io.battlerune.game.world.entity.combat.effect.impl.CombatVenomEffect;
import io.battlerune.game.world.entity.combat.hit.CombatHit;
import io.battlerune.game.world.entity.combat.hit.Hit;
import io.battlerune.game.world.entity.combat.projectile.CombatProjectile;
import io.battlerune.game.world.entity.combat.strategy.basic.MagicStrategy;
import io.battlerune.game.world.entity.actor.Actor;
import io.battlerune.game.world.entity.actor.npc.Npc;
import io.battlerune.util.RandomUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class NpcMagicStrategy extends MagicStrategy<Npc> {
	
	protected final CombatProjectile combatProjectile;
	
	/**
	 * The spell splash graphic.
	 */
	private static final Graphic SPLASH = new Graphic(85);
	
	public NpcMagicStrategy(CombatProjectile combatProjectile) {
		this.combatProjectile = combatProjectile;
	}
	
	@Override
	public void start(Npc attacker, Actor defender, Hit[] hits) {
		Animation animation = getAttackAnimation(attacker, defender);
		
		if(combatProjectile.getAnimation().isPresent() && (animation.getId() == -1 || animation.getId() == 65535)) {
			animation = combatProjectile.getAnimation().get();
		}
		
		attacker.animate(animation);
		combatProjectile.getStart().ifPresent(attacker::graphic);
		combatProjectile.sendProjectile(attacker, defender);
		
		for(Hit hit : hits) {
			Predicate<CombatImpact> filter = effect -> effect.canAffect(attacker, defender, hit);
			Consumer<CombatImpact> execute = effect -> effect.impact(attacker, defender, hit, null);
			combatProjectile.getEffect().filter(filter).ifPresent(execute);
			
			if(!attacker.definition.isPoisonous()) {
				continue;
			}
			
			if(CombatVenomEffect.isVenomous(attacker) && RandomUtils.success(0.25)) {
				defender.venom();
			} else {
				CombatPoisonEffect.getPoisonType(attacker.id).ifPresent(defender::poison);
			}
		}
	}
	
	@Override
	public void hit(Npc attacker, Actor defender, Hit hit) {
		if(!hit.isAccurate()) {
			defender.graphic(SPLASH);
		} else {
			combatProjectile.getEnd().ifPresent(defender::graphic);
		}
	}
	
	@Override
	public CombatHit[] getHits(Npc attacker, Actor defender) {
		return new CombatHit[]{nextMagicHit(attacker, defender, combatProjectile)};
	}
	
	@Override
	public int getAttackDelay(Npc attacker, Actor defender, FightType fightType) {
		int delay = attacker.definition.getAttackDelay();
		
		if(attacker.getPosition().getDistance(defender.getPosition()) > 4) {
			return 1 + delay;
		}
		
		return delay;
	}
	
	@Override
	public int getAttackDistance(Npc attacker, FightType fightType) {
		return 10;
	}
	
	@Override
	public Animation getAttackAnimation(Npc attacker, Actor defender) {
		return new Animation(attacker.definition.getAttackAnimation(), UpdatePriority.HIGH);
	}
	
	@Override
	public boolean canAttack(Npc attacker, Actor defender) {
		return true;
	}
	
	@Override
	public CombatType getCombatType() {
		return CombatType.MAGIC;
	}
	
}