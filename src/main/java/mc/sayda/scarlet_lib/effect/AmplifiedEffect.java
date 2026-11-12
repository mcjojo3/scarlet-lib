package mc.sayda.scarlet_lib.effect;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.modificationstation.stationapi.api.effect.EntityEffect;

/**
 * A status effect with what StationAPI's effects lack: an amplifier (level - 1), and an absorption
 * pool that soaks damage before health (absorption, shield-style effects). StationAPI ticks,
 * saves and syncs it. Behaviour comes from the {@link EffectBehavior} it was registered with.
 */
public abstract class AmplifiedEffect extends EntityEffect<AmplifiedEffect> {
    /** Amplifier handed to the factory by {@link Effects#apply}. */
    static final ThreadLocal<Integer> PENDING_AMPLIFIER = ThreadLocal.withInitial(() -> 0);

    private int amplifier;
    private double pool;
    private int age;

    protected AmplifiedEffect(Entity entity, int ticks) {
        super(entity, ticks);
        this.amplifier = PENDING_AMPLIFIER.get();
    }

    public int getAmplifier() {
        return amplifier;
    }

    /** Damage this effect can still absorb. */
    public double getPool() {
        return pool;
    }

    public void setPool(double pool) {
        this.pool = pool;
    }

    /** Ticks since the effect was added. */
    public int getAge() {
        return age;
    }

    public Entity getEntity() {
        return entity;
    }

    @Override
    public void onAdded(boolean appliedNow) {
        EffectTypes.behavior(getType()).onAdded(this, appliedNow);
    }

    @Override
    public void onTick() {
        age++;
        EffectTypes.behavior(getType()).onTick(this);
    }

    @Override
    public void onRemoved() {
        EffectTypes.behavior(getType()).onRemoved(this);
    }

    @Override
    protected void writeNbt(NbtCompound tag) {
        tag.putInt("Amplifier", amplifier);
        tag.putDouble("Pool", pool);
    }

    @Override
    protected void readNbt(NbtCompound tag) {
        amplifier = tag.getInt("Amplifier");
        pool = tag.getDouble("Pool");
    }
}
