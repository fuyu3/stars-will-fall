package com.fuyu.starswillfall.guardasol;

/**
 * Todos os números de balanceamento do guarda-sol ficam aqui.
 * Tempos estão em ticks (20 ticks = 1 segundo); velocidades em blocos por tick.
 */
public final class GuardaSolTuning {
    private GuardaSolTuning() {
    }

    // ---- Durabilidade ------------------------------------------------------------------------
    public static final int MAX_DURABILITY = 1200;

    // ---- Formas ------------------------------------------------------------------------------
    /** Bônus de dano (o dano total é 1 + este valor). Guarda-sol aberto: fraco, é uma ferramenta. */
    public static final double OPEN_DAMAGE = 1.0D;
    public static final double OPEN_ATTACK_SPEED = -2.0D;
    /** Espada: 5 de bônus = 6 de dano total (o mesmo de uma espada de ferro). */
    public static final double CLOSED_DAMAGE = 5.0D;
    public static final double CLOSED_ATTACK_SPEED = -2.4D;
    /** Tempo de espera depois de abrir/fechar, para não dar spam de troca. */
    public static final int TOGGLE_COOLDOWN_TICKS = 10;

    // ---- Florescer (forma aberta, clique direito num bloco) ----------------------------------
    public static final int BLOOM_RADIUS = 3;
    public static final int BLOOM_MAX_FLOWERS = 14;
    /** Chance de cada coluna do círculo receber uma flor. */
    public static final float BLOOM_CHANCE = 0.6F;
    public static final int BLOOM_COOLDOWN_TICKS = 30;
    public static final int BLOOM_DURABILITY_COST = 1;

    // ---- Planar / voar (forma aberta, segurar clique direito) --------------------------------
    /** Velocidade do impulso inicial (clique com os pés no chão), na direção do olhar. */
    public static final double LAUNCH_SPEED = 1.0D;
    /** Subida mínima do impulso, para descolar do chão mesmo olhando reto ou para baixo. */
    public static final double LAUNCH_MIN_UP = 0.3D;
    /** Velocidade máxima de queda enquanto plana (0.10 = 2 blocos/s). */
    public static final double GLIDE_FALL_SPEED = 0.10D;
    /** Velocidade horizontal ao planar (0.38 = 7,6 blocos/s). */
    public static final double GLIDE_SPEED = 0.38D;
    /** Quão rápido a velocidade acompanha para onde o jogador olha (0 a 1). */
    public static final double GLIDE_STEER = 0.12D;
    /** Duração máxima de um planeio contínuo (160 = 8 s). */
    public static final int GLIDE_MAX_TICKS = 160;
    /** Recarga aplicada ao soltar/terminar o planeio. */
    public static final int GLIDE_COOLDOWN_TICKS = 40;
    /** A cada quantos ticks planando se gasta 1 de durabilidade. */
    public static final int GLIDE_DURABILITY_INTERVAL = 40;

    // ---- Névoa de flores (forma fechada/espada, clique direito) ------------------------------
    /** Distância máxima em que a névoa pode ser lançada. */
    public static final double MIST_RANGE = 6.0D;
    public static final double MIST_RADIUS = 3.0D;
    /** Quanto tempo a névoa dura (160 = 8 s). */
    public static final int MIST_DURATION_TICKS = 160;
    public static final int MIST_COOLDOWN_TICKS = 240;
    /** De quantos em quantos ticks os efeitos são reaplicados a quem está dentro. */
    public static final int MIST_EFFECT_INTERVAL = 10;
    public static final int NAUSEA_TICKS = 200;
    public static final int POISON_TICKS = 100;
    /** 0 = Veneno I, 1 = Veneno II. O veneno do jogo nunca mata, só deixa em 1 de vida. */
    public static final int POISON_AMPLIFIER = 1;
    /** Se false, a névoa só afeta mobs (útil em servidor sem PvP). */
    public static final boolean MIST_AFFECTS_PLAYERS = true;
    public static final int MIST_DURABILITY_COST = 2;
}
