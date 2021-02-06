package data.scripts.weapons;
//vayra helped a lot with this btw

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.Color;

import org.lwjgl.util.vector.Vector2f;

public class NWP_depolarizationbeameffect implements BeamEffectPlugin {

    private final IntervalUtil fireInterval = new IntervalUtil(.1f, .1f);
    private boolean wasZero = true;
    private int damTick = 0;
    private static final Vector2f ZERO = new Vector2f();

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        CombatEntityAPI target = beam.getDamageTarget();

        if (target instanceof ShipAPI && beam.getBrightness() >= 1f) {

            float maxFlux = ((ShipAPI) target).getFluxTracker().getMaxFlux();
            boolean hasResistantConduits = ((ShipAPI) target).getVariant().hasHullMod("fluxbreakers");
            float TICK_COUNT = ((maxFlux/500)+20);
            if  (hasResistantConduits) {
                TICK_COUNT = (((maxFlux/500)+20)*1.25f);
            }

            boolean shieldHit = target.getShield() != null && target.getShield().isWithinArc(beam.getTo());
            if (!shieldHit || target.getShield() == null) {
                float dur = beam.getDamage().getDpsDuration();
                // needed because when the ship is in fast-time, dpsDuration will not be reset every frame as it should be
                if (!wasZero) {
                    dur = 0;
                }
                wasZero = beam.getDamage().getDpsDuration() <= 0;
                fireInterval.advance(dur);

                if (fireInterval.intervalElapsed()) {
                    Global.getCombatEngine().addFloatingText(new Vector2f(beam.getFrom()), "flux level is " + ((ShipAPI) target).getCurrFlux(), 50f, Color.yellow, null, 1f, 0f);

                    damTick++;
                    if (damTick >= TICK_COUNT) {
                        if (((ShipAPI) target).getFluxTracker().isOverloaded()) {
                            ((ShipAPI) target).getFluxTracker().stopOverload();
                        }

                        float maxDamage = 50f + ((((ShipAPI) target).getCurrFlux()-((ShipAPI) target).getHardFluxLevel())
                        + ((ShipAPI) target).getHardFluxLevel()*1.3f);
                        float radius = 1f + (maxDamage/25);
                        float coreRadius = 1f + (maxDamage/20);
                        float minDamage = maxDamage-5;
                 //       float pitch = 0.1f

                        DamagingExplosionSpec spec = new DamagingExplosionSpec(1f, radius, coreRadius, maxDamage, minDamage,
                                CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
                                CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
                                10f,
                                10f,
                                1f,
                                0,
                                Color.magenta, Color.magenta);
                        spec.setDamageType(DamageType.ENERGY);
                        engine.spawnDamagingExplosion(spec, beam.getSource(), beam.getTo());
                        Global.getSoundPlayer().playSound("swp_ionblaster_hit", 1.1f, 0.6f, beam.getTo(), ZERO);
                        ((ShipAPI) target).getFluxTracker().decreaseFlux(((ShipAPI) target).getCurrFlux());

         //               Global.getCombatEngine().addFloatingText(new Vector2f(beam.getFrom()), "thing work", 200f, Color.yellow, null, 1f, 0f);
                        damTick = 0;
                    }
                }
            }
          else {
                damTick = 0;
            }
        }
        if (target == null) {
            damTick = 0;
        }
    }
}