/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.task;

import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.PlayerStorageData;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ClaimBlockTask extends BukkitRunnable {

    public ClaimBlockTask() {
        this.runTaskTimer(GDBootstrap.getInstance(), 1L, 20L * 60 * 5);
    }

    @Override
    public void run() {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Player player : world.getPlayers()) {
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                final GDPermissionUser holder = PermissionHolderCache.getInstance().getOrCreateUser(player);
                final int accrualPerHour = GDPermissionManager.getInstance().getInternalOptionValue(holder, Options.BLOCKS_ACCRUED_PER_HOUR, claim, playerData).intValue();
                if (accrualPerHour > 0) {
                    Location lastLocation = playerData.lastAfkCheckLocation;
                    // if he's not in a vehicle and has moved at least three blocks since the last check and he's not being pushed around by fluids
                    if (player.getVehicle() == null &&
                            (lastLocation == null || lastLocation.getWorld() != player.getWorld() || lastLocation.distanceSquared(player.getLocation()) >= 0) &&
                            !NMSUtil.getInstance().isBlockWater(player.getLocation().getBlock())) {
                        int accruedBlocks = playerData.getBlocksAccruedPerHour() / 12;
                        if (accruedBlocks < 0) {
                            accruedBlocks = 1;
                        }

                        int currentTotal = playerData.getAccruedClaimBlocks();
                        if ((currentTotal + accruedBlocks) > playerData.getMaxAccruedClaimBlocks()) {
                            PlayerStorageData playerStorage = playerData.getStorageData();
                            playerStorage.getConfig().setAccruedClaimBlocks(playerData.getMaxAccruedClaimBlocks());
                            playerData.lastAfkCheckLocation = player.getLocation();
                            return;
                        }

                        PlayerStorageData playerStorage = playerData.getStorageData();
                        playerStorage.getConfig().setAccruedClaimBlocks(playerStorage.getConfig().getAccruedClaimBlocks() + accruedBlocks);
                    }

                    playerData.lastAfkCheckLocation = player.getLocation();
                }
            }
        }
    }
}
