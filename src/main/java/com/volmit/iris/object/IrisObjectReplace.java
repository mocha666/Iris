package com.volmit.iris.object;

import org.bukkit.block.data.BlockData;

import com.volmit.iris.gen.atomics.AtomicCache;
import com.volmit.iris.manager.IrisDataManager;
import com.volmit.iris.noise.CNG;
import com.volmit.iris.util.ArrayType;
import com.volmit.iris.util.Desc;
import com.volmit.iris.util.DontObfuscate;
import com.volmit.iris.util.KList;
import com.volmit.iris.util.RNG;
import com.volmit.iris.util.Required;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Desc("Find and replace object materials")
@Data
public class IrisObjectReplace
{
	@ArrayType(min = 1, type = IrisBlockData.class)
	@Required
	@Desc("Find this block")
	@DontObfuscate
	private KList<IrisBlockData> find = new KList<>();

	@Required
	@Desc("Replace it with this block palette")
	@DontObfuscate
	private IrisMaterialPalette replace = new IrisMaterialPalette();

	@Desc("Exactly match the block data or not")
	@DontObfuscate
	private boolean exact = false;

	private final transient AtomicCache<CNG> replaceGen = new AtomicCache<>();
	private final transient AtomicCache<KList<BlockData>> findData = new AtomicCache<>();
	private final transient AtomicCache<KList<BlockData>> replaceData = new AtomicCache<>();

	public KList<BlockData> getFind(IrisDataManager rdata)
	{
		return findData.aquire(() ->
		{
			KList<BlockData> b = new KList<>();

			for(IrisBlockData i : find)
			{
				BlockData bx = i.getBlockData(rdata);

				if(bx != null)
				{
					b.add(bx);
				}
			}

			return b;
		});
	}

	public BlockData getReplace(RNG seed, double x, double y, double z, IrisDataManager rdata)
	{
		return getReplace().get(seed, x, y, z, rdata);
	}
}
