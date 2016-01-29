package sonar.logistics.integration.multipart;

import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraftforge.common.util.ForgeDirection;
import sonar.core.integration.fmp.SonarTilePart;
import sonar.core.utils.BlockCoords;
import sonar.logistics.api.connecting.IMultiDataCable;
import sonar.logistics.client.renderers.RenderHandlers;
import sonar.logistics.helpers.CableHelper;
import sonar.logistics.registries.BlockRegistry;
import codechicken.lib.vec.Cuboid6;
import codechicken.multipart.NormallyOccludedPart;
import codechicken.multipart.TMultiPart;

public class MultiDataCablePart extends SonarTilePart implements IMultiDataCable {

	public int registryID = -1;
	
	public boolean occlusion;

	// taken from Applied Energistics Code
	final double SHORTER = 6.0 / 16.0;
	final double LONGER = 10.0 / 16.0;
	final double MIN_DIRECTION = 0;
	final double MAX_DIRECTION = 1.0;
	final Cuboid6[] SIDE_TESTS = {

			// DOWN(0, -1, 0),
			new Cuboid6(SHORTER, MIN_DIRECTION, SHORTER, LONGER, SHORTER, LONGER),

			// UP(0, 1, 0),
			new Cuboid6(SHORTER, LONGER, SHORTER, LONGER, MAX_DIRECTION, LONGER),

			// NORTH(0, 0, -1),
			new Cuboid6(SHORTER, SHORTER, MIN_DIRECTION, LONGER, LONGER, SHORTER),

			// SOUTH(0, 0, 1),
			new Cuboid6(SHORTER, SHORTER, LONGER, LONGER, LONGER, MAX_DIRECTION),

			// WEST(-1, 0, 0),
			new Cuboid6(MIN_DIRECTION, SHORTER, SHORTER, SHORTER, LONGER, LONGER),

			// EAST(1, 0, 0),
			new Cuboid6(LONGER, SHORTER, SHORTER, MAX_DIRECTION, LONGER, LONGER), };

	public MultiDataCablePart() {
		super();
	}

	public MultiDataCablePart(int meta) {
		super(meta);
	}

	@Override
	public Cuboid6 getBounds() {
		return new Cuboid6((float) 0.0625 * 6, (float) 0.0625 * 6, (float) 0.0625 * 6, (float) (1 - (0.0625 * 6)), (float) (1 - (0.0625 * 6)), (float) (1 - (0.0625 * 6)));
	}

	@Override
	public Object getSpecialRenderer() {
		return new RenderHandlers.BlockMultiCable();
	}

	@Override
	public Block getBlock() {
		return BlockRegistry.dataMultiCable;
	}

	@Override
	public String getType() {
		return "Multi Cable Part";
	}

	@Override
	public Iterable<Cuboid6> getOcclusionBoxes() {
		if (this.occlusion) {
			return Collections.emptyList();
		}
		return super.getOcclusionBoxes();

	}

	@Override
	public boolean isBlocked(final ForgeDirection side) {
		if (side == null || side == ForgeDirection.UNKNOWN || this.tile() == null) {
			return false;
		}

		occlusion = true;
		boolean blocked = !this.tile().canAddPart(new NormallyOccludedPart(SIDE_TESTS[side.ordinal()]));
		occlusion = false;

		return blocked;
	}

	public int canRenderConnection(ForgeDirection dir) {
		if (this.isBlocked(dir)) {
			return 0;
		}
		return CableHelper.canRenderConnection(tile(), dir);
	}
	
	@Override
	public BlockCoords getCoords() {
		return new BlockCoords(tile());
	}

	@Override
	public void onWorldJoin() {
		super.onWorldJoin();
		CableHelper.addCable(this);
	}

	@Override
	public void onWorldSeparate() {
		super.onWorldSeparate();
		CableHelper.removeCable(this);
	}

	public void onPartChanged(TMultiPart part) {
		CableHelper.removeCable(this);
		super.onPartChanged(part);
		CableHelper.addCable(this);
	}

	@Override
	public int registryID() {
		return registryID;
	}

	@Override
	public void setRegistryID(int id) {
		this.registryID = id;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return true;
	}

	@Override
	public boolean unlimitedChannels() {
		return true;
	}
}
