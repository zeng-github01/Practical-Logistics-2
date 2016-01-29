package sonar.logistics.common.tileentity;

import net.minecraftforge.common.util.ForgeDirection;
import sonar.core.common.tileentity.TileEntityHandler;
import sonar.core.integration.fmp.FMPHelper;
import sonar.core.integration.fmp.handlers.TileHandler;
import sonar.core.network.utils.ITextField;
import sonar.core.utils.BlockCoords;
import sonar.logistics.api.Info;
import sonar.logistics.api.connecting.IInfoEmitter;
import sonar.logistics.api.render.ICableRenderer;
import sonar.logistics.common.handlers.DataModifierHandler;
import sonar.logistics.helpers.CableHelper;

public class TileEntityDataModifier extends TileEntityHandler implements IInfoEmitter, ICableRenderer, ITextField {

	public DataModifierHandler handler = new DataModifierHandler(false, this);

	@Override
	public TileHandler getTileHandler() {
		return handler;
	}

	@Override
	public boolean canConnect(ForgeDirection dir) {
		return handler.canConnect(dir);
	}

	@Override
	public Info currentInfo() {
		return handler.currentInfo();
	}

	public boolean maxRender() {
		return true;
	}

	@Override
	public void textTyped(String string, int id) {
		handler.textTyped(string, id);		
	}

	@Override
	public int canRenderConnection(ForgeDirection dir) {
		return handler.canRenderConnection(dir, this);
	}

	@Override
	public BlockCoords getCoords() {
		return new BlockCoords(this);
	}

	
	@Override
	public void addConnections() {
		for (int i = 0; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.getOrientation(i);
			if (dir != ForgeDirection.getOrientation(FMPHelper.getMeta(this)).getOpposite()) {
				CableHelper.addConnection(this, dir);
			}
		}
	}

	@Override
	public void removeConnections() {
		for (int i = 0; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.getOrientation(i);
			if (dir != ForgeDirection.getOrientation(FMPHelper.getMeta(this)).getOpposite()) {
				CableHelper.removeConnection(this, dir);
			}
		}
	}
}
