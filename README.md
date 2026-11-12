# Scarlet Lib

Modern vanilla mechanics for Minecraft b1.7.3 (Babric + StationAPI), packaged so any mod can use them.

- **Status effects**: 27 vanilla effects (speed, regeneration, night vision, darkness, ...) with amplifiers, built on StationAPI's effect system, so they save, sync and show icons.
- **Attributes**: the 1.21 attribute model (max health, movement speed, attack damage, armor, scale, step height, jump strength, gravity, interaction ranges, ...) applied where beta uses the matching number.
- **Damage types**: typed, fractional damage with a modifier/listener pipeline.
- **Entity data**: a saved NBT compound on every entity, for any mod's own data.
- **Body size**: player hitbox, eye height and model scale, with the follow-on fixes (fluids, pickup reach, bed reach, the server's stance check).
- **Visibility**: rules for who can see whom (rendering and mob targeting).

Mod id `scarlet_lib`, package `mc.sayda.scarlet_lib`. Requires StationAPI.

## Depending on it

It isn't on a public Maven yet. Either build it from source next to your mod (as CreRaces and
Twilight Lib do), or publish it with `gradlew publishToMavenLocal` and add `mavenLocal()`.

```kotlin
// settings.gradle.kts: from source, with this repository cloned next to yours
include("scarlet-lib")
project(":scarlet-lib").projectDir = file("../scarlet-lib")

// build.gradle.kts
dependencies {
    implementation(project(path = ":scarlet-lib", configuration = "namedElements"))
    include(project(":scarlet-lib")) // optional: ship it inside your jar
    // or, from mavenLocal: modImplementation("mc.sayda.scarlet_lib:scarlet-lib:0.1.0+b1.7.3")
}
```

```json
// fabric.mod.json
"depends": { "scarlet_lib": "*" }
```

## Effects

```java
Effects.apply(entity, Identifier.of("minecraft:speed"), 200, 1); // Speed II for 10 seconds
Effects.has(entity, "minecraft:night_vision");
Effects.amplifier(entity, "minecraft:strength");                  // -1 when absent
```

To add your own effect, register it from StationAPI's effect registry event:

```java
@EventListener
private static void registerEffects(EntityEffectTypeRegistryEvent event) {
    EffectTypes.register(MyMod.NAMESPACE.id("bleeding"), new EffectBehavior() {
        @Override
        public void onTick(AmplifiedEffect effect) { /* ... */ }
    });
}
```

Put the icon in `assets/<namespace>/stationapi/textures/gui/effect/<path>.png`, and the name and the one-line description in your lang file as `gui.stationapi.effect.<namespace>.<path>.name` and `.desc`.

An effect with `setPool(n)` soaks `n` damage, as absorption does; such pools are used up before absorption. Override `absorbs(effect, damageType)` to block only some damage types, and `onPoolEmptied(effect)` to react when the pool runs out (a shield breaking).

`Effects.addApplyFilter` can veto effects (immunities).

## Attributes

```java
double speed = Attributes.value(player, Attributes.MOVEMENT_SPEED);
Attributes.addModifier(player, Attributes.MAX_HEALTH,
        new AttributeModifier(MyMod.NAMESPACE.id("bonus"), 4, AttributeModifier.Operation.ADD_VALUE)); // saved with the entity
```

For modifiers that follow your own state (equipment, effects, player data), register a provider. It runs on every read, so keep it cheap:

```java
Attributes.addProvider((entity, attribute, sink) -> {
    if (attribute == Attributes.JUMP_STRENGTH && isRooted(entity))
        sink.accept(new AttributeModifier(MyMod.NAMESPACE.id("rooted"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
});
```

Your own attributes: `Attributes.register(new Attribute(id, default, min, max))`.

## Damage

```java
Damage.deal(victim, attacker, 2.5, DamageTypes.MAGIC); // fractions carry over between hits
Damage.heal(entity, 0.5);
```

Every hit on a living entity runs this pipeline: fire resistance, then your `Damage.addModifier` hooks (return 0 to cancel), then resistance and absorption pools. After the hit lands, `Damage.addListener` hooks run. Use `Damage.addHealModifier` to change healing, and `Damage.addAttackListener` for player melee hits (life steal, on-hit effects). Beta's damage has no type, so vanilla hits are given one from their cause: fire, lava, fall, drown, in_wall, out_of_world, player_attack, mob_attack, arrow, thrown, fireball, explosion, cactus and lightning_bolt (see `DamageTypes`).

## Commands

Scarlet Lib adds `/effect give|clear` and `/attribute` (the 1.21 forms, for players). It also gives other mods a small command API. Beta has no command framework, so Scarlet Lib hands typed commands from singleplayer chat (which it also enables), server chat and the server console to whatever is registered:

```java
Commands.register("mycommand", Commands.Access.OPERATORS, (source, args) -> {
    for (PlayerEntity target : Commands.targets(source, args[0])) { /* a name, @s, @p or @a */ }
    source.reply("Done");
}).usage("/mycommand <player>");
```

Unregistered commands are left to vanilla and other mods.

With [Retro Commands](https://modrinth.com/mod/retrocommands) installed, Retro Commands runs the commands instead: everything registered here is added to it (its permissions, `/help` and config apply) and Scarlet Lib's chat and console hooks stand down.

## Entity data, size, visibility

- `EntityData.of(entity)` returns a saved `NbtCompound`. Namespace your keys.
- `BodySize.addProvider(player -> new BodySize.Size(...))` multiplies a player's hitbox, eye height and model size. The `generic.scale` attribute scales everything uniformly. Walking animation, footsteps and view bobbing are paced by model height, so a small body's legs move at a natural rate.
- `Visibility.addRule((viewer, target) -> hidden)` hides `target` from `viewer` (null means mobs in general) for rendering and targeting.

## Compatibility notes

Scarlet Lib changes beta behaviour with chainable injectors (wraps and multipliers) where it can, so other mods' mixins on the same methods still apply. If your mod also changes something Scarlet Lib owns (reach, step height, max health, ...), prefer an attribute modifier over your own mixin, so the two stack.
