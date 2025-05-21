package fr.crafity.dropper.data;

import fr.crafity.dropper.Dropper;
import org.bukkit.entity.Player;

public class PlayerData {
    private int lives;
    private boolean eliminated;
    private int jumpsThisTurn = 0;
    private int perfectDropsThisTurn = 0;

    private int missedTurns = 0;
    private boolean pendingSuperJump = false;
    private int disconnectCount = 0; // ✅ Remplace disconnectedOnce pour une vraie gestion de nombre de déconnexions

    public PlayerData(Player player) {
        this.lives = Dropper.getInstance().getConfig().getInt("default_lives", 3);
        this.eliminated = false;
    }

    // ✅ Déconnexions
    public int getDisconnectCount() {
        return disconnectCount;
    }

    public void incrementDisconnects() {
        disconnectCount++;
    }

    public void resetDisconnects() {
        disconnectCount = 0;
    }

    // ✅ Tours manqués
    public int getMissedTurns() {
        return missedTurns;
    }

    public void incrementMissedTurns() {
        missedTurns++;
    }

    public void setMissedTurns(int missed) {
        missedTurns = missed;
    }

    public void resetMissedTurns() {
        missedTurns = 0;
    }

    // ✅ Super Saut
    public boolean hasPendingSuperJump() {
        return pendingSuperJump;
    }

    public void setPendingSuperJump(boolean value) {
        pendingSuperJump = value;
    }

    // ✅ Vies et État
    public void loseLife() {
        if (eliminated) return;
        lives--;
        if (lives <= 0) {
            eliminated = true;
        }
    }

    public void addLife() {
        if (eliminated) return;
        lives++;
        if (lives <= 0) {
            eliminated = true;
        }
    }

    public void setLives(int life) {
        if (eliminated) return;
        lives = life;
        if (lives <= 0) {
            eliminated = true;
        }
    }

    public int getLives() {
        return lives;
    }

    public boolean isEliminated() {
        return eliminated;
    }

    public void setEliminated(boolean eliminated) {
        this.eliminated = eliminated;
    }

    // ✅ Sauts
    public int getJumpsThisTurn() {
        return jumpsThisTurn;
    }

    public void incrementJumpsThisTurn() {
        jumpsThisTurn++;
    }

    public void setJumpsThisTurn(int jumps) {
        jumpsThisTurn = jumps;
    }

    public void resetJumpsThisTurn() {
        jumpsThisTurn = 0;
    }

    public int getPerfectDropsThisTurn() {
        return perfectDropsThisTurn;
    }

    public void incrementPerfectDropsThisTurn() {
        perfectDropsThisTurn++;
    }

    public void setPerfectDropsThisTurn(int jumpsP) {
        perfectDropsThisTurn = jumpsP;
    }

    public void resetPerfectDropsThisTurn() {
        perfectDropsThisTurn = 0;
    }

    // ✅ Reset complet des données du joueur
    public void reset(int defaultLives) {
        this.lives = defaultLives;
        this.eliminated = false;
        this.jumpsThisTurn = 0;
        this.perfectDropsThisTurn = 0;
        this.missedTurns = 0;
        this.pendingSuperJump = false;
        this.disconnectCount = 0;
    }
}
