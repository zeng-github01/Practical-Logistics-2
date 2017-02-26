package sonar.logistics.info.types;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sonar.core.client.gui.GuiSonar;
import sonar.core.helpers.NBTHelper;
import sonar.core.helpers.RenderHelper;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.core.network.sync.BaseSyncListPart;
import sonar.core.network.sync.ICheckableSyncPart;
import sonar.core.network.sync.IDirtyPart;
import sonar.core.network.sync.ISyncPart;
import sonar.core.network.sync.ISyncableListener;
import sonar.core.network.sync.SyncableList;
import sonar.logistics.api.displays.DisplayButton;
import sonar.logistics.api.displays.IDisplayInfo;
import sonar.logistics.api.displays.InfoContainer;
import sonar.logistics.api.displays.ScreenInteractionEvent;
import sonar.logistics.api.info.IMonitorInfo;
import sonar.logistics.client.gui.GuiInventoryReader;

/** typical implementation of IMonitorInfo which has a sync parts list for all the Info things it also has the required constructor which required empty constructor */
public abstract class BaseInfo<T extends IMonitorInfo> extends BaseSyncListPart implements IMonitorInfo<T>, ISyncableListener {

	public BaseInfo() {
	}

	@Override
	public boolean isHeader() {
		return false;
	}

	public boolean equals(Object object) {
		if (object != null && object instanceof IMonitorInfo) {
			IMonitorInfo info = (IMonitorInfo) object;
			return (info.isHeader() && isHeader()) || (this.isMatchingType(info) && isMatchingInfo((T) info) && isIdenticalInfo((T) info));
		}
		return false;
	}

	@Override
	public void identifyChanges(T newInfo) {
		ArrayList<ISyncPart> parts = syncList.getStandardSyncParts();
		ArrayList<ISyncPart> infoParts = syncList.getStandardSyncParts();

		for (int i = 0; i < parts.size(); i++) {
			ISyncPart toCheck = infoParts.get(i);
			if (toCheck instanceof ICheckableSyncPart) {
				if (!((ICheckableSyncPart) parts.get(i)).equalPart(toCheck)) {
					toCheck.getListener().markChanged(toCheck);
				}
			} else {
				toCheck.getListener().markChanged(toCheck);
			}
		}
	}

	/// BUTTONS///

	@SideOnly(Side.CLIENT)
	public ArrayList<DisplayButton> buttons = new ArrayList();
	public boolean displayMenu = false;

	@SideOnly(Side.CLIENT)
	public void renderButtons(InfoContainer container, IDisplayInfo displayInfo, double width, double height, double scale, int infoPos) {
		Minecraft.getMinecraft().getTextureManager().bindTexture(GuiInventoryReader.sorting_icons);
		GL11.glPushMatrix();
		GL11.glTranslated(-1 + (0.0625 * 1), -1 + 0.0625 * 5, 0);
		GlStateManager.translate((width / 2) - 0.16 + (-0.20*(buttons.size()+1)), (height / 2) - 0.16, 0);
		GlStateManager.scale(0.02, 0.02, 0.02);
		for (DisplayButton button : buttons) {
			GlStateManager.translate(20, 0, 0);
			RenderHelper.drawTexturedModalRect(0, 0, button.texX, button.texY, 16, 16);
		}
		// RenderHelper.drawTexturedModalRect(0, 0, 0, 0, 15 + 1, 15 + 1);
		// GlStateManager.scale(1/0.00625, 1/0.00625, 1/0.00625);
		GL11.glPopMatrix();
	}

	public NBTTagCompound onButtonClickedClient(ScreenInteractionEvent event, IDisplayInfo renderInfo, EntityPlayer player, ItemStack stack, InfoContainer container) {
		NBTTagCompound clickTag = new NBTTagCompound();
		
		return clickTag;

	}

	public void onButtonEvent(InfoContainer container, IDisplayInfo displayInfo, ScreenInteractionEvent event, NBTTagCompound clickTag) {
	}

	public void resetButtons() {
		buttons = new ArrayList();
		this.getButtons(buttons);
	}

	@SideOnly(Side.CLIENT)
	public void getButtons(ArrayList<DisplayButton> buttons) {
	}

	@SideOnly(Side.CLIENT)
	public void onButtonClicked(String buttonID) {

	}

}
