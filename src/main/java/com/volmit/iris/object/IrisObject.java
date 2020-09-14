package com.volmit.iris.object;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.util.BlockVector;

import com.volmit.iris.Iris;
import com.volmit.iris.manager.IrisDataManager;
import com.volmit.iris.util.B;
import com.volmit.iris.util.BlockPosition;
import com.volmit.iris.util.CarveResult;
import com.volmit.iris.util.ChunkPosition;
import com.volmit.iris.util.IObjectPlacer;
import com.volmit.iris.util.IrisLock;
import com.volmit.iris.util.KMap;
import com.volmit.iris.util.RNG;

import lombok.Data;
import lombok.EqualsAndHashCode;

import lombok.experimental.Accessors;

@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class IrisObject extends IrisRegistrant
{
	private static final BlockData AIR = B.getBlockData("CAVE_AIR");
	private static final BlockData VAIR = B.getBlockData("VOID_AIR");
	private static final BlockData VAIR_DEBUG = B.getBlockData("COBWEB");
	private static final BlockData[] SNOW_LAYERS = new BlockData[] {B.getBlockData("minecraft:snow[layers=1]"), B.getBlockData("minecraft:snow[layers=2]"), B.getBlockData("minecraft:snow[layers=3]"), B.getBlockData("minecraft:snow[layers=4]"), B.getBlockData("minecraft:snow[layers=5]"), B.getBlockData("minecraft:snow[layers=6]"), B.getBlockData("minecraft:snow[layers=7]"), B.getBlockData("minecraft:snow[layers=8]")};
	public static boolean shitty = false;
	private KMap<BlockVector, BlockData> blocks;
	private int w;
	private int d;
	private int h;
	private transient BlockVector center;
	private transient volatile boolean smartBored = false;
	private transient IrisLock lock = new IrisLock("Preloadcache");

	public void ensureSmartBored(boolean debug)
	{
		if(smartBored)
		{
			return;
		}

		lock.lock();
		int applied = 0;
		if(blocks.isEmpty())
		{
			lock.unlock();
			Iris.warn("Cannot Smart Bore " + getLoadKey() + " because it has 0 blocks in it.");
			smartBored = true;
			return;
		}

		BlockVector max = new BlockVector(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
		BlockVector min = new BlockVector(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);

		for(BlockVector i : blocks.k())
		{
			max.setX(i.getX() > max.getX() ? i.getX() : max.getX());
			min.setX(i.getX() < min.getX() ? i.getX() : min.getX());
			max.setY(i.getY() > max.getY() ? i.getY() : max.getY());
			min.setY(i.getY() < min.getY() ? i.getY() : min.getY());
			max.setZ(i.getZ() > max.getZ() ? i.getZ() : max.getZ());
			min.setZ(i.getZ() < min.getZ() ? i.getZ() : min.getZ());
		}

		// Smash X
		for(int rayY = min.getBlockY(); rayY <= max.getBlockY(); rayY++)
		{
			for(int rayZ = min.getBlockZ(); rayZ <= max.getBlockZ(); rayZ++)
			{
				int start = Integer.MAX_VALUE;
				int end = Integer.MIN_VALUE;

				for(int ray = min.getBlockX(); ray <= max.getBlockX(); ray++)
				{
					if(blocks.containsKey(new BlockVector(ray, rayY, rayZ)))
					{
						start = ray < start ? ray : start;
						end = ray > end ? ray : end;
					}
				}

				if(start != Integer.MAX_VALUE && end != Integer.MIN_VALUE)
				{
					for(int i = start; i <= end; i++)
					{
						BlockVector v = new BlockVector(i, rayY, rayZ);

						if(!blocks.containsKey(v) || B.isAir(blocks.get(v)))
						{
							blocks.put(v, debug ? VAIR_DEBUG : VAIR);
							applied++;
						}
					}
				}
			}
		}

		// Smash Y
		for(int rayX = min.getBlockX(); rayX <= max.getBlockX(); rayX++)
		{
			for(int rayZ = min.getBlockZ(); rayZ <= max.getBlockZ(); rayZ++)
			{
				int start = Integer.MAX_VALUE;
				int end = Integer.MIN_VALUE;

				for(int ray = min.getBlockY(); ray <= max.getBlockY(); ray++)
				{
					if(blocks.containsKey(new BlockVector(rayX, ray, rayZ)))
					{
						start = ray < start ? ray : start;
						end = ray > end ? ray : end;
					}
				}

				if(start != Integer.MAX_VALUE && end != Integer.MIN_VALUE)
				{
					for(int i = start; i <= end; i++)
					{
						BlockVector v = new BlockVector(rayX, i, rayZ);

						if(!blocks.containsKey(v) || B.isAir(blocks.get(v)))
						{
							blocks.put(v, debug ? VAIR_DEBUG : VAIR);
							applied++;
						}
					}
				}
			}
		}

		// Smash Z
		for(int rayX = min.getBlockX(); rayX <= max.getBlockX(); rayX++)
		{
			for(int rayY = min.getBlockY(); rayY <= max.getBlockY(); rayY++)
			{
				int start = Integer.MAX_VALUE;
				int end = Integer.MIN_VALUE;

				for(int ray = min.getBlockZ(); ray <= max.getBlockZ(); ray++)
				{
					if(blocks.containsKey(new BlockVector(rayX, rayY, ray)))
					{
						start = ray < start ? ray : start;
						end = ray > end ? ray : end;
					}
				}

				if(start != Integer.MAX_VALUE && end != Integer.MIN_VALUE)
				{
					for(int i = start; i <= end; i++)
					{
						BlockVector v = new BlockVector(rayX, rayY, i);

						if(!blocks.containsKey(v) || B.isAir(blocks.get(v)))
						{
							blocks.put(v, debug ? VAIR_DEBUG : VAIR);
							applied++;
						}
					}
				}
			}
		}

		Iris.verbose("- Applied Smart Bore to " + getLoadKey() + " Filled with " + applied + " VOID_AIR blocks.");

		smartBored = true;
		lock.unlock();
	}

	public IrisObject copy()
	{
		IrisObject o = new IrisObject(w, h, d);
		o.setLoadKey(o.getLoadKey());
		o.setCenter(getCenter().clone());

		for(BlockVector i : getBlocks().k())
		{
			o.getBlocks().put(i.clone(), getBlocks().get(i).clone());
		}

		return o;
	}

	public IrisObject(int w, int h, int d)
	{
		blocks = new KMap<>();
		this.w = w;
		this.h = h;
		this.d = d;
		center = new BlockVector(w / 2, h / 2, d / 2);
	}

	@SuppressWarnings("resource")
	public static BlockVector sampleSize(File file) throws IOException
	{
		FileInputStream in = new FileInputStream(file);
		DataInputStream din = new DataInputStream(in);
		BlockVector bv = new BlockVector(din.readInt(), din.readInt(), din.readInt());
		Iris.later(() -> din.close());
		return bv;
	}

	public void read(InputStream in) throws IOException
	{
		if(shitty)
		{
			return;
		}

		DataInputStream din = new DataInputStream(in);
		this.w = din.readInt();
		this.h = din.readInt();
		this.d = din.readInt();
		center = new BlockVector(w / 2, h / 2, d / 2);
		int s = din.readInt();

		for(int i = 0; i < s; i++)
		{
			blocks.put(new BlockVector(din.readShort(), din.readShort(), din.readShort()), B.getBlockData(din.readUTF()));
		}
	}

	public void read(File file) throws IOException
	{
		if(shitty)
		{
			return;
		}
		FileInputStream fin = new FileInputStream(file);
		read(fin);
		fin.close();
	}

	public void write(File file) throws IOException
	{
		if(shitty)
		{
			return;
		}
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		write(out);
		out.close();
	}

	public void write(OutputStream o) throws IOException
	{
		if(shitty)
		{
			return;
		}
		DataOutputStream dos = new DataOutputStream(o);
		dos.writeInt(w);
		dos.writeInt(h);
		dos.writeInt(d);
		dos.writeInt(blocks.size());
		for(BlockVector i : blocks.k())
		{
			dos.writeShort(i.getBlockX());
			dos.writeShort(i.getBlockY());
			dos.writeShort(i.getBlockZ());
			dos.writeUTF(blocks.get(i).getAsString(true));
		}
	}

	public void clean()
	{
		if(shitty)
		{
			return;
		}
		KMap<BlockVector, BlockData> d = blocks.copy();
		blocks.clear();

		for(BlockVector i : d.k())
		{
			blocks.put(new BlockVector(i.getBlockX(), i.getBlockY(), i.getBlockZ()), d.get(i));
		}
	}

	public void setUnsigned(int x, int y, int z, BlockData block)
	{
		if(shitty)
		{
			return;
		}
		if(x >= w || y >= h || z >= d)
		{
			throw new RuntimeException(x + " " + y + " " + z + " exceeds limit of " + w + " " + h + " " + d);
		}

		BlockVector v = new BlockVector(x, y, z).subtract(center).toBlockVector();

		if(block == null)
		{
			blocks.remove(v);
		}

		else
		{
			blocks.put(v, block);
		}
	}

	public void place(int x, int z, IObjectPlacer placer, IrisObjectPlacement config, RNG rng, IrisDataManager rdata)
	{
		if(shitty)
		{
			return;
		}
		place(x, -1, z, placer, config, rng,rdata);
	}

	public void place(int x, int z, IObjectPlacer placer, IrisObjectPlacement config, RNG rng, CarveResult c, IrisDataManager rdata)
	{
		if(shitty)
		{
			return;
		}
		place(x, -1, z, placer, config, rng, null, c,rdata);
	}

	public int place(int x, int yv, int z, IObjectPlacer placer, IrisObjectPlacement config, RNG rng, IrisDataManager rdata)
	{
		return place(x, yv, z, placer, config, rng, null, null,rdata);
	}

	public int place(int x, int yv, int z, IObjectPlacer placer, IrisObjectPlacement config, RNG rng, Consumer<BlockPosition> listener, CarveResult c, IrisDataManager rdata)
	{
		if(config.isSmartBore())
		{
			ensureSmartBored(placer.isDebugSmartBore());
		}

		boolean warped = !config.getWarp().isFlat();
		boolean stilting = (config.getMode().equals(ObjectPlaceMode.STILT) || config.getMode().equals(ObjectPlaceMode.FAST_STILT));
		KMap<ChunkPosition, Integer> heightmap = config.getSnow() > 0 ? new KMap<>() : null;
		int spinx = rng.imax() / 1000;
		int spiny = rng.imax() / 1000;
		int spinz = rng.imax() / 1000;
		int rty = config.getRotation().rotate(new BlockVector(0, getCenter().getBlockY(), 0), spinx, spiny, spinz).getBlockY();
		int ty = config.getTranslate().translate(new BlockVector(0, getCenter().getBlockY(), 0), config.getRotation(), spinx, spiny, spinz).getBlockY();
		int y = -1;
		int xx, zz;
		int yrand = config.getTranslate().getYRandom();
		yrand = yrand > 0 ? rng.i(0, yrand) : yrand < 0 ? rng.i(yrand, 0) : yrand;

		if(yv < 0)
		{
			if(config.getMode().equals(ObjectPlaceMode.CENTER_HEIGHT))
			{
				y = (c != null ? c.getSurface() : placer.getHighest(x, z, config.isUnderwater())) + rty;
			}

			else if(config.getMode().equals(ObjectPlaceMode.MAX_HEIGHT) || config.getMode().equals(ObjectPlaceMode.STILT))
			{
				BlockVector offset = new BlockVector(config.getTranslate().getX(), config.getTranslate().getY(), config.getTranslate().getZ());
				BlockVector rotatedDimensions = config.getRotation().rotate(new BlockVector(getW(), getH(), getD()), spinx, spiny, spinz).clone();

				for(int i = x - (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i <= x + (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i++)
				{
					for(int j = z - (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j <= z + (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j++)
					{
						int h = placer.getHighest(i, j, config.isUnderwater()) + rty;

						if(h > y)
						{
							y = h;
						}
					}
				}
			}

			else if(config.getMode().equals(ObjectPlaceMode.FAST_MAX_HEIGHT) || config.getMode().equals(ObjectPlaceMode.FAST_STILT))
			{
				BlockVector offset = new BlockVector(config.getTranslate().getX(), config.getTranslate().getY(), config.getTranslate().getZ());
				BlockVector rotatedDimensions = config.getRotation().rotate(new BlockVector(getW(), getH(), getD()), spinx, spiny, spinz).clone();

				for(int i = x - (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i <= x + (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i += (rotatedDimensions.getBlockX() / 2))
				{
					for(int j = z - (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j <= z + (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j += (rotatedDimensions.getBlockZ() / 2))
					{
						int h = placer.getHighest(i, j, config.isUnderwater()) + rty;

						if(h > y)
						{
							y = h;
						}
					}
				}
			}

			else if(config.getMode().equals(ObjectPlaceMode.MIN_HEIGHT))
			{
				y = 257;
				BlockVector offset = new BlockVector(config.getTranslate().getX(), config.getTranslate().getY(), config.getTranslate().getZ());
				BlockVector rotatedDimensions = config.getRotation().rotate(new BlockVector(getW(), getH(), getD()), spinx, spiny, spinz).clone();

				for(int i = x - (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i <= x + (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i++)
				{
					for(int j = z - (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j <= z + (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j++)
					{
						int h = placer.getHighest(i, j, config.isUnderwater()) + rty;

						if(h < y)
						{
							y = h;
						}
					}
				}
			}

			else if(config.getMode().equals(ObjectPlaceMode.FAST_MIN_HEIGHT))
			{
				y = 257;
				BlockVector offset = new BlockVector(config.getTranslate().getX(), config.getTranslate().getY(), config.getTranslate().getZ());
				BlockVector rotatedDimensions = config.getRotation().rotate(new BlockVector(getW(), getH(), getD()), spinx, spiny, spinz).clone();

				for(int i = x - (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i <= x + (rotatedDimensions.getBlockX() / 2) + offset.getBlockX(); i += (rotatedDimensions.getBlockX() / 2))
				{
					for(int j = z - (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j <= z + (rotatedDimensions.getBlockZ() / 2) + offset.getBlockZ(); j += (rotatedDimensions.getBlockZ() / 2))
					{
						int h = placer.getHighest(i, j, config.isUnderwater()) + rty;

						if(h < y)
						{
							y = h;
						}
					}
				}
			}

			else if(config.getMode().equals(ObjectPlaceMode.PAINT))
			{
				y = placer.getHighest(x, z, config.isUnderwater()) + rty;
			}
		}

		else
		{
			y = yv;
		}

		if(yv >= 0 && config.isBottom())
		{
			y += Math.floorDiv(h, 2);
		}

		if(yv < 0)
		{
			if(!config.isUnderwater() && !config.isOnwater() && placer.isUnderwater(x, z))
			{
				return -1;
			}
		}

		if(c != null && Math.max(0, h + yrand + ty) + 1 >= c.getHeight())
		{
			return -1;
		}

		if(config.isUnderwater() && y + rty + ty >= placer.getFluidHeight())
		{
			return -1;
		}

		if(!config.getClamp().canPlace(y + rty + ty, y - rty + ty))
		{
			return -1;
		}

		if(config.isBore())
		{
			for(int i = x - Math.floorDiv(w, 2); i <= x + Math.floorDiv(w, 2) - (w % 2 == 0 ? 1 : 0); i++)
			{
				for(int j = y - Math.floorDiv(h, 2) - config.getBoarExtendMinY(); j <= y + Math.floorDiv(h, 2) + config.getBoarExtendMaxY() - (h % 2 == 0 ? 1 : 0); j++)
				{
					for(int k = z - Math.floorDiv(d, 2); k <= z + Math.floorDiv(d, 2) - (d % 2 == 0 ? 1 : 0); k++)
					{
						placer.set(i, j, k, AIR);
					}
				}
			}
		}

		int lowest = Integer.MAX_VALUE;
		y += yrand;
		for(BlockVector g : blocks.keySet())
		{
			BlockVector i = g.clone();
			i = config.getRotation().rotate(i.clone(), spinx, spiny, spinz).clone();
			i = config.getTranslate().translate(i.clone(), config.getRotation(), spinx, spiny, spinz).clone();
			BlockData data = blocks.get(g).clone();

			if(stilting && i.getBlockY() < lowest && !B.isAir(data))
			{
				lowest = i.getBlockY();
			}

			if(placer.isPreventingDecay() && data instanceof Leaves && !((Leaves) data).isPersistent())
			{
				((Leaves) data).setPersistent(true);
			}

			for(IrisObjectReplace j : config.getEdit())
			{
				for(BlockData k : j.getFind(rdata))
				{
					if(j.isExact() ? k.matches(data) : k.getMaterial().equals(data.getMaterial()))
					{
						data = j.getReplace(rng, i.getX() + x, i.getY() + y, i.getZ() + z,rdata).clone();
					}
				}
			}

			data = config.getRotation().rotate(data, spinx, spiny, spinz);
			xx = x + (int) Math.round(i.getX());
			int yy = y + (int) Math.round(i.getY());
			zz = z + (int) Math.round(i.getZ());

			if(warped)
			{
				xx += config.warp(rng, i.getX() + x, i.getY() + y, i.getZ() + z);
				zz += config.warp(rng, i.getZ() + z, i.getY() + y, i.getX() + x);
			}

			if(yv < 0 && config.getMode().equals(ObjectPlaceMode.PAINT))
			{
				yy = (int) Math.round(i.getY()) + Math.floorDiv(h, 2) + placer.getHighest(xx, zz, config.isUnderwater());
			}

			if(heightmap != null)
			{
				ChunkPosition pos = new ChunkPosition(xx, zz);

				if(!heightmap.containsKey(pos))
				{
					heightmap.put(pos, yy);
				}

				if(heightmap.get(pos) < yy)
				{
					heightmap.put(pos, yy);
				}
			}

			if(config.isMeld() && !placer.isSolid(xx, yy, zz))
			{
				continue;
			}

			if(config.isWaterloggable() && yy <= placer.getFluidHeight() && data instanceof Waterlogged)
			{
				((Waterlogged) data).setWaterlogged(true);
			}

			if(listener != null)
			{
				listener.accept(new BlockPosition(xx, yy, zz));
			}

			if(!data.getMaterial().equals(Material.AIR) && !data.getMaterial().equals(Material.CAVE_AIR))
			{
				placer.set(xx, yy, zz, data);
			}
		}

		if(stilting)
		{
			for(BlockVector g : blocks.keySet())
			{
				BlockVector i = g.clone();
				i = config.getRotation().rotate(i.clone(), spinx, spiny, spinz).clone();
				i = config.getTranslate().translate(i.clone(), config.getRotation(), spinx, spiny, spinz).clone();

				if(i.getBlockY() != lowest)
				{
					continue;
				}

				BlockData d = blocks.get(i);

				if(d == null || B.isAir(d))
				{
					continue;
				}

				xx = x + (int) Math.round(i.getX());
				zz = z + (int) Math.round(i.getZ());

				if(warped)
				{
					xx += config.warp(rng, i.getX() + x, i.getY() + y, i.getZ() + z);
					zz += config.warp(rng, i.getZ() + z, i.getY() + y, i.getX() + x);
				}

				int yg = placer.getHighest(xx, zz, config.isUnderwater());

				if(yv >= 0 && config.isBottom())
				{
					y += Math.floorDiv(h, 2);
				}

				for(int j = lowest + y; j > yg - config.getOverStilt() - 1; j--)
				{
					placer.set(xx, j, zz, d);
				}
			}
		}

		if(heightmap != null)
		{
			RNG rngx = rng.nextParallelRNG(3468854);

			for(ChunkPosition i : heightmap.k())
			{
				int vx = i.getX();
				int vy = heightmap.get(i);
				int vz = i.getZ();

				if(config.getSnow() > 0)
				{
					int height = rngx.i(0, (int) (config.getSnow() * 7));
					placer.set(vx, vy + 1, vz, SNOW_LAYERS[Math.max(Math.min(height, 7), 0)]);
				}
			}
		}

		return y;
	}

	public void rotate(IrisObjectRotation r, int spinx, int spiny, int spinz)
	{
		if(shitty)
		{
			return;
		}

		KMap<BlockVector, BlockData> v = blocks.copy();
		blocks.clear();

		for(BlockVector i : v.keySet())
		{
			blocks.put(r.rotate(i.clone(), spinx, spiny, spinz), r.rotate(v.get(i).clone(), spinx, spiny, spinz));
		}
	}

	public void place(Location at)
	{
		if(shitty)
		{
			return;
		}

		for(BlockVector i : blocks.keySet())
		{
			at.clone().add(0, getCenter().getY(), 0).add(i).getBlock().setBlockData(blocks.get(i), false);
		}
	}
}
