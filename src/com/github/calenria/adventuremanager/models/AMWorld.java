package com.github.calenria.adventuremanager.models;

import java.util.Date;

import org.bukkit.World;

public class AMWorld {

    public World world;
    public Integer activePlayers = 0;

    public Date lastReset;
    public Date lastPlayerLeave;
    public Date lastPlayerJoin;
    public Boolean resetSinceLastPlayerLeft;

    
    public String toString() {
        return world.getName();
    }
    
}
