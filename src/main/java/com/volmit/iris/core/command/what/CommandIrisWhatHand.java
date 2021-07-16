/*
 * Iris is a World Generator for Minecraft Bukkit Servers
 * Copyright (c) 2021 Arcane Arts (Volmit Software)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.volmit.iris.core.command.what;

import com.volmit.iris.Iris;
import com.volmit.iris.util.C;
import com.volmit.iris.util.KList;
import com.volmit.iris.util.MortarCommand;
import com.volmit.iris.util.MortarSender;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

public class CommandIrisWhatHand extends MortarCommand {
    public CommandIrisWhatHand() {
        super("hand", "h");
        setDescription("Get the block data for holding.");
        requiresPermission(Iris.perm.studio);
        setCategory("Wut");
        setDescription("What block am I holding");
    }

    @Override
    public void addTabOptions(MortarSender sender, String[] args, KList<String> list) {

    }

    @Override
    public boolean handle(MortarSender sender, String[] args) {
        if (sender.isPlayer()) {
            Player p = sender.player();
            try {
                BlockData bd = p.getInventory().getItemInMainHand().getType().createBlockData();
                if (!bd.getMaterial().equals(Material.AIR)) {
                    sender.sendMessage("Material: " + C.GREEN + bd.getMaterial().name());
                    sender.sendMessage("Full: " + C.WHITE + bd.getAsString(true));
                } else {
                    sender.sendMessage("Please hold a block/item");
                }
            } catch (Throwable e) {
                Iris.reportError(e);
                Material bd = p.getInventory().getItemInMainHand().getType();
                if (!bd.equals(Material.AIR)) {
                    sender.sendMessage("Material: " + C.GREEN + bd.name());
                } else {
                    sender.sendMessage("Please hold a block/item");
                }
            }
        } else {
            sender.sendMessage("Players only.");
        }

        return true;
    }

    @Override
    protected String getArgsUsage() {
        return "";
    }
}