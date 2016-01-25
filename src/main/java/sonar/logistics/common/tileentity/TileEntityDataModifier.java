package sonar.logistics.common.tileentity;

import net.minecraftforge.common.util.ForgeDirection;
import sonar.core.common.tileentity.TileEntityHandler;
import sonar.core.integration.fmp.handlers.TileHandler;
import sonar.core.network.utils.ITextField;
import sonar.logistics.api.Info;
import sonar.logistics.api.connecting.IDataConnection;
import sonar.logistics.api.render.ICableRenderer;
import sonar.logistics.common.handlers.DataModifierHandler;

public class TileEntityDataModifier extends TileEntityHandler implements IDataConnection, ICableRenderer, ITextField {

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
	public boolean canRenderConnection(ForgeDirection dir) {
		return handler.canRenderConnection(this, dir);
	}
}
