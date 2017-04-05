package sonar.logistics.common.multiparts;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import mcmultipart.MCMultiPartMod;
import mcmultipart.multipart.IRedstonePart;
import mcmultipart.raytrace.PartMOP;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import sonar.core.api.IFlexibleGui;
import sonar.core.helpers.FontHelper;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.core.integration.multipart.SonarMultipartHelper;
import sonar.core.inventory.ContainerMultipartSync;
import sonar.core.network.sync.SyncTagType;
import sonar.core.network.utils.IByteBufTile;
import sonar.logistics.PL2;
import sonar.logistics.PL2Items;
import sonar.logistics.api.cabling.NetworkConnectionType;
import sonar.logistics.api.info.IMonitorInfo;
import sonar.logistics.api.info.InfoUUID;
import sonar.logistics.api.readers.IInfoProvider;
import sonar.logistics.api.utils.LogisticsHelper;
import sonar.logistics.api.viewers.ILogicViewable;
import sonar.logistics.api.viewers.ViewerTally;
import sonar.logistics.api.viewers.ViewerType;
import sonar.logistics.api.viewers.ViewersList;
import sonar.logistics.client.gui.GuiClock;
import sonar.logistics.info.types.ClockInfo;

public class ClockPart extends SidedMultipart implements IInfoProvider, IRedstonePart, IByteBufTile, IFlexibleGui, ILogicViewable {

	public ViewersList viewers = new ViewersList(this, ViewerType.ALL);
	public static final PropertyBool HAND = PropertyBool.create("hand");

	public long lastMillis;// when the movement was started
	public long currentMillis;// the current millis
	public long offset = 0;

	public SyncTagType.LONG tickTime = new SyncTagType.LONG(1);

	public float rotation;// 0-360 indicating rotation of the clock hand.
	public boolean isSet;

	public boolean lastSignal;
	public boolean wasStarted;
	public boolean powering;

	public long finalStopTime;
	{
		this.syncList.addParts(tickTime);
	}

	public ClockPart() {
		super(3 * 0.0625, 0.0625 * 1, 0.0625 * 3);
	}

	public ClockPart(EnumFacing face) {
		super(face, 5 * 0.0625, 0.0625 * 1, 0.0625 * 3);
	}

	@Override
	public boolean onActivated(EntityPlayer player, EnumHand hand, ItemStack heldItem, PartMOP hit) {
		if (!LogisticsHelper.isPlayerUsingOperator(player)) {
			if (!getWorld().isRemote) {
				openFlexibleGui(player, 0);
			}
			return true;
		}
		return false;
	}

	public void update() {
		super.update();
		if (isClient()) {
			return;
		}
		currentMillis = (getWorld().getTotalWorldTime() * 50);
		if (!(tickTime.getObject() < 10)) {
			long start = currentMillis - lastMillis;
			rotation = (start) * 360 / (tickTime.getObject());
			if (start > tickTime.getObject()) {
				this.lastMillis = currentMillis;
				powering = true;
				notifyBlockUpdate();
				// send signal
			} else {
				if (powering) {
					powering = false;
					notifyBlockUpdate();
				}
			}
			markDirty();
		}
		sendByteBufPacket(0);
		setClockInfo();
	}

	@Override
	public NetworkConnectionType canConnect(EnumFacing dir) {
		return (dir == face.getObject() || dir.getOpposite() == face.getObject()) ? NetworkConnectionType.NETWORK : NetworkConnectionType.NONE;
	}

	public void setClockInfo() {
		IMonitorInfo info = null;
		if (!(tickTime.getObject() < 10)) {
			long start = currentMillis - lastMillis;
			String timeString = new SimpleDateFormat("HH:mm:ss:SSS").format((start) - (60 * 60 * 1000)).substring(0, 11);
			info = new ClockInfo(start, tickTime.getObject(), timeString);
		}

		if (info != null) {
			InfoUUID id = new InfoUUID(getIdentity().hashCode(), 0);
			IMonitorInfo oldInfo = PL2.getServerManager().info.get(id);
			if (oldInfo == null || !oldInfo.isMatchingType(info) || !oldInfo.isMatchingInfo(info) || !oldInfo.isIdenticalInfo(info)) {
				PL2.getServerManager().changeInfo(id, info);
			}
		}
	}

	//// IInfoProvider \\\\

	@Override
	public IMonitorInfo getMonitorInfo(int pos) {
		return PL2.getServerManager().getInfoFromUUID(new InfoUUID(getIdentity().hashCode(), 0));
	}

	public UUID getIdentity() {
		return getUUID();
	}

	@Override
	public int getMaxInfo() {
		return 1;
	}

	//// ILogicViewable \\\\

	public ViewersList getViewersList() {
		return viewers;
	}

	@Override
	public void onViewerAdded(EntityPlayer player, List<ViewerTally> type) {
		SonarMultipartHelper.sendMultipartSyncToPlayer(this, (EntityPlayerMP) player);
	}

	@Override
	public void onViewerRemoved(EntityPlayer player, List<ViewerTally> type) {
	}

	//// STATE \\\\

	@Override
	public IBlockState getActualState(IBlockState state) {
		return state.withProperty(ORIENTATION, getFacing()).withProperty(HAND, false);
	}

	public BlockStateContainer createBlockState() {
		return new BlockStateContainer(MCMultiPartMod.multipart, new IProperty[] { ORIENTATION, HAND });
	}

	//// EVENTS \\\\

	public void onLoaded() {
		super.onLoaded();
		PL2.getInfoManager(this.getWorld().isRemote).addMonitor(this);
		if (isServer()) {
			setClockInfo();
		}
	}

	public void onRemoved() {
		super.onRemoved();
		PL2.getInfoManager(this.getWorld().isRemote).removeMonitor(this);
	}

	public void onUnloaded() {
		super.onUnloaded();
		PL2.getInfoManager(this.getWorld().isRemote).removeMonitor(this);
	}

	public void onFirstTick() {
		super.onFirstTick();
		PL2.getInfoManager(this.getWorld().isRemote).addMonitor(this);
	}

	//// SAVING \\\\

	public void readData(NBTTagCompound nbt, SyncType type) {
		super.readData(nbt, type);
		tickTime.readData(nbt, type);
		if (type == SyncType.SAVE) {
			nbt.setBoolean("isSet", isSet);
			nbt.setBoolean("lastSignal", lastSignal);
			nbt.setBoolean("wasStarted", wasStarted);
			nbt.setLong("finalStopTime", finalStopTime);
		}

	}

	public NBTTagCompound writeData(NBTTagCompound nbt, SyncType type) {
		super.writeData(nbt, type);
		tickTime.writeData(nbt, type);
		if (type == SyncType.SAVE) {
			this.isSet = nbt.getBoolean("isSet");
			this.lastSignal = nbt.getBoolean("lastSignal");
			this.wasStarted = nbt.getBoolean("wasStarted");
			this.finalStopTime = nbt.getLong("finalStopTime");
		}
		return nbt;
	}

	//// PACKETS \\\\

	@Override
	public void writePacket(ByteBuf buf, int id) {
		switch (id) {
		case 0:
			buf.writeFloat(rotation);
			break;
		case 1:
			tickTime.writeToBuf(buf);
			break;
		}
	}

	@Override
	public void readPacket(ByteBuf buf, int id) {
		switch (id) {
		case -4:
			sendByteBufPacket(-2);
			break;
		case 0:
			rotation = buf.readFloat();
			break;
		case 1:
			tickTime.readFromBuf(buf);
			break;
		case 2:
			tickTime.increaseBy(100);
			break;
		case 3:
			tickTime.increaseBy(-100);
			break;
		case 4:
			tickTime.increaseBy(1000);
			break;
		case 5:
			tickTime.increaseBy(-1000);
			break;
		case 6:
			tickTime.increaseBy(60000);
			break;
		case 7:
			tickTime.increaseBy(-60000);
			break;
		case 8:
			tickTime.increaseBy(60000 * 60);
			break;
		case 9:
			tickTime.increaseBy(-(60000 * 60));
			break;
		}
		if (tickTime.getObject() < 0) {
			tickTime.setObject((long) 0);
		} else if (tickTime.getObject() > (1000 * 60 * 60 * 24)) {
			tickTime.setObject((long) ((1000 * 60 * 60 * 24) - 1));
		}
	}

	//// GUI \\\\

	@Override
	public Object getServerElement(Object obj, int id, World world, EntityPlayer player, NBTTagCompound tag) {
		return id == 0 ? new ContainerMultipartSync(this) : null;
	}

	@Override
	public Object getClientElement(Object obj, int id, World world, EntityPlayer player, NBTTagCompound tag) {
		return id == 0 ? new GuiClock(this) : null;
	}

	@Override
	public void onGuiOpened(Object obj, int id, World world, EntityPlayer player, NBTTagCompound tag) {
		switch (id) {
		case 0:
			SonarMultipartHelper.sendMultipartSyncToPlayer(this, (EntityPlayerMP) player);
			break;
		}
	}

	@Override
	public ItemStack getItemStack() {
		return new ItemStack(PL2Items.clock);
	}

	@Override
	public String getDisplayName() {
		return FontHelper.translate("item.Clock.name");
	}

	@Override
	public boolean canConnectRedstone(EnumFacing side) {
		return side != getFacing().getOpposite();
	}

	@Override
	public int getWeakSignal(EnumFacing side) {
		return (side != getFacing().getOpposite() && powering) ? 15 : 0;
	}

	@Override
	public int getStrongSignal(EnumFacing side) {
		return (side != getFacing().getOpposite() && powering) ? 15 : 0;
	}

}