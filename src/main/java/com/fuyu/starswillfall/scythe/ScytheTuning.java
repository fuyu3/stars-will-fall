package com.fuyu.starswillfall.scythe;

/**
 * Números de balanceamento da foice. Tempos em ticks (20 = 1 s); velocidades em blocos por tick.
 */
public final class ScytheTuning {
    private ScytheTuning() {
    }

    // ---- Bumerangue (clique direito) ---------------------------------------------------------
    /** Dano de cada acerto; igual ao golpe corpo a corpo (1 + 3 + 5). Encantamentos somam. */
    public static final float BOOMERANG_DAMAGE = 9.0F;
    /** Velocidade de saída. */
    public static final double BOOMERANG_SPEED = 1.3D;
    /** Ticks indo em frente antes de voltar (14 ticks ≈ 12 blocos de alcance). */
    public static final int BOOMERANG_OUT_TICKS = 14;
    /** Fração da velocidade que sobra no fim da ida (a foice vai freando). */
    public static final double BOOMERANG_END_SPEED_FACTOR = 0.35D;
    /** Volta: velocidade inicial, aceleração por tick, teto e quão rápido ela curva para você. */
    public static final double BOOMERANG_RETURN_START_SPEED = 0.6D;
    public static final double BOOMERANG_RETURN_ACCEL = 0.08D;
    public static final double BOOMERANG_RETURN_MAX_SPEED = 1.6D;
    public static final double BOOMERANG_RETURN_STEER = 0.35D;
    /** A que distância do jogador ela é "pega" de volta. */
    public static final double BOOMERANG_CATCH_DISTANCE = 1.4D;
    /** Depois disso (15 s) ela é entregue ao dono de qualquer jeito. */
    public static final int BOOMERANG_MAX_LIFETIME_TICKS = 300;
    /** Durabilidade gasta por alvo atingido. */
    public static final int BOOMERANG_HIT_DURABILITY = 1;
    /** Giro visual da foice voando, em graus por tick. */
    public static final float BOOMERANG_SPIN_DEGREES = 48.0F;

    // ---- Troca de modo (agachar + clique direito) --------------------------------------------
    /** Pausa depois de trocar de modo, para segurar o botão não ficar alternando. */
    public static final int MODE_SWITCH_COOLDOWN_TICKS = 10;

    // ---- Impulso estelar (modo impulso, clique direito) --------------------------------------
    public static final double DASH_SPEED = 1.5D;
    public static final double DASH_MIN_UP = 0.3D;
    /**
     * Sem recarga, como o impulso do guarda-sol. O que impede voar para sempre é só poder dar o
     * impulso com os pés no chão. Ponha false para liberar no ar (aí vira voo livre: use com cuidado).
     */
    public static final boolean DASH_GROUND_ONLY = true;
    /** Depois do impulso o jogador não leva dano de queda por este tempo (estrelas amortecem). */
    public static final int DASH_SAFE_FALL_TICKS = 40;
    public static final int DASH_DURABILITY_COST = 1;
}
