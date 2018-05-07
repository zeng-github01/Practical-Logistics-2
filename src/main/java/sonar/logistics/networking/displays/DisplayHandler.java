package sonar.logistics.networking.displays;

import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import sonar.core.helpers.ListHelper;
import sonar.core.helpers.NBTHelper;
import sonar.core.utils.Pair;
import sonar.logistics.PL2;
import sonar.logistics.api.cabling.CableConnectionType;
import sonar.logistics.api.cabling.ConnectableType;
import sonar.logistics.api.cabling.ICableConnectable;
import sonar.logistics.api.displays.DisplayGSI;
import sonar.logistics.api.displays.storage.DisplayGSISaveHandler;
import sonar.logistics.api.displays.tiles.ConnectedDisplay;
import sonar.logistics.api.displays.tiles.IDisplay;
import sonar.logistics.api.displays.tiles.ILargeDisplay;
import sonar.logistics.api.displays.tiles.ISmallDisplay;
import sonar.logistics.api.utils.PL2AdditionType;
import sonar.logistics.api.utils.PL2RemovalType;
import sonar.logistics.common.multiparts.holographic.TileAbstractHolographicDisplay;
import sonar.logistics.networking.ServerInfoHandler;
import sonar.logistics.networking.cabling.AbstractConnectionHandler;
import sonar.logistics.networking.cabling.CableHelper;
import sonar.logistics.packets.PacketConnectedDisplayUpdate;
import sonar.logistics.worlddata.GSIData;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayHandler extends AbstractConnectionHandler<ILargeDisplay> {

	public static DisplayHandler instance() {
		return PL2.proxy.server_display_manager;
	}

	public final Map<Integer, List<ConnectedDisplayChange>> display_updates = new HashMap<>();
	public final Map<Integer, TileAbstractHolographicDisplay> holographic_displays = new HashMap<>();
	public List<Integer> rebuild = Lists.newArrayList();

	@Override
	public void removeAll(){
		super.removeAll();
		rebuild.clear();
		display_updates.clear();
		holographic_displays.clear();
	}

	public void updateConnectedDisplays() {
		if (!display_updates.isEmpty()) {
			Map<Integer, ConnectedDisplay> connected = ServerInfoHandler.instance().getConnectedDisplays();
			if (!connected.isEmpty()) {
				connected.values().forEach(this::runChanges);
			}
			display_updates.clear();
		}
	}

	public void onDisplayAddition(ILargeDisplay display) {
		ListHelper.addWithCheck(rebuild, display.getRegistryID());
		addConnectionToNetwork(display);
		ListHelper.addWithCheck(rebuild, display.getRegistryID());
	}

	public void onDisplayRemoval(ILargeDisplay display) {
		ListHelper.addWithCheck(rebuild, display.getRegistryID());
		removeConnectionFromNetwork(display);
		ListHelper.addWithCheck(rebuild, display.getRegistryID());
	}

	public void addDisplay(IDisplay display, PL2AdditionType type){
	    if(display instanceof ISmallDisplay) {
			ISmallDisplay screen = (ISmallDisplay) display;
			screen.setGSI(new DisplayGSI(display, display.getActualWorld(), display.getInfoContainerID()));
            NBTTagCompound tag = GSIData.unloadedGSI.get(display.getInfoContainerID());
            if(tag != null){
                screen.getGSI().readData(tag, NBTHelper.SyncType.SAVE);
            }
        }

        DisplayGSI gsi = display.getGSI();
        if (gsi != null && gsi.getDisplay() != null && !ServerInfoHandler.instance().displays.containsValue(gsi)) {
            validateGSI(display, gsi);
        }
    }

    public void removeDisplay(IDisplay display, PL2RemovalType type){
        if(display instanceof ISmallDisplay) {
			ISmallDisplay screen = (ISmallDisplay) display;
            if(type == PL2RemovalType.PLAYER_REMOVED){
                GSIData.unloadedGSI.remove(display.getInfoContainerID());
            }else{
                GSIData.unloadedGSI.put(display.getInfoContainerID(), display.getGSI().writeData(new NBTTagCompound(), NBTHelper.SyncType.SAVE));
            }
        }
        DisplayGSI gsi = display.getGSI();
        if (gsi != null && gsi.getDisplay() != null) {
            invalidateGSI(display, gsi);
        }
    }

    public void validateGSI(IDisplay display, DisplayGSI gsi) {
        if (display == gsi.getDisplay().getActualDisplay()) {
            gsi.validate();
            gsi.sendInfoContainerPacket(DisplayGSISaveHandler.DisplayGSISavedData.ALL_DATA);
			ChunkViewerHandler.instance().onDisplayAdded(gsi);
        }
    }

    public void invalidateGSI(IDisplay display, DisplayGSI gsi) {
        gsi.invalidate();
		ChunkViewerHandler.instance().onDisplayRemoved(gsi);
    }

	public void createConnectedDisplays() {
		for (Integer i : rebuild) {
			ConnectedDisplay display = ServerInfoHandler.instance().getConnectedDisplays().get(i);
			List<ILargeDisplay> displays = getConnections(i);
			if (displays.isEmpty()) {
				if (display != null) {
					invalidateGSI(display, display.getGSI());
				}
				ServerInfoHandler.instance().getConnectedDisplays().remove(i);
			} else if (display == null) {
				ILargeDisplay first_display = displays.get(0);
				World world = first_display.getActualWorld();
				ConnectedDisplay connectedDisplay = ConnectedDisplay.loadDisplay(world, i);
				ServerInfoHandler.instance().getConnectedDisplays().put(i, connectedDisplay);
				connectedDisplay.face.setObject(first_display.getCableFace());
				connectedDisplay.setDisplayScaling();
				connectedDisplay.getGSI().validate();
				markConnectedDisplayChanged(i, ConnectedDisplayChange.SUB_NETWORK_CHANGED, ConnectedDisplayChange.SUB_DISPLAY_CHANGED, ConnectedDisplayChange.WATCHERS_CHANGED);
			} else {
				markConnectedDisplayChanged(i, ConnectedDisplayChange.SUB_DISPLAY_CHANGED, ConnectedDisplayChange.WATCHERS_CHANGED);
			}
		}
	}

	public void markConnectedDisplayChanged(int registryID, @Nonnull ConnectedDisplayChange... changes) {
		display_updates.putIfAbsent(registryID, new ArrayList<>());
		for (ConnectedDisplayChange change : changes) {
			if (!display_updates.get(registryID).contains(change)) {
				display_updates.get(registryID).add(change);
			}
		}
	}

	public List<ConnectedDisplayChange> getChanges(int registryID) {
		return display_updates.getOrDefault(registryID, new ArrayList<>());
	}

	public void runChanges(ConnectedDisplay display) {
		List<ConnectedDisplayChange> changes = getChanges(display.getRegistryID());
		for (ConnectedDisplayChange change : ConnectedDisplayChange.values()) { // keeps order, and allows other changes to be during another
			if (change.shouldRunChange(changes) && !change.doChange(changes, display)) {
				return;
			}
		}
	}

	public static boolean updateLargeDisplays(List<ConnectedDisplayChange> changes, ConnectedDisplay display) {
		List<ILargeDisplay> displays = DisplayHandler.instance().getConnections(display.getRegistryID());
		if (!displays.isEmpty()) {
			display.setDisplayScaling();
			DisplayHandler.instance().markConnectedDisplayChanged(display.getRegistryID(), ConnectedDisplayChange.WATCHERS_CHANGED, ConnectedDisplayChange.SUB_NETWORK_CHANGED);
			return true;
		}
		// FIXME - no tiles are loaded, therefore, we either mark as being UNLOADED, OR WE DELETE ...

		return false; // if the count = 0 don't continue updates.

	}

	public static boolean updateConnectedNetworks(List<ConnectedDisplayChange> changes, ConnectedDisplay display) {
		display.getGSI().validateAllInfoReferences();
		return true;
	}

	public static boolean updateWatchers(List<ConnectedDisplayChange> changes, ConnectedDisplay display) {
		if (display.getCoords() != null) { // this shouldn't happen, but if it does it'll break the ChunkViewerHandler
			List<EntityPlayerMP> watchers = ChunkViewerHandler.instance().getWatchingPlayers(display.getGSI());
			watchers.forEach(watcher -> PL2.network.sendTo(new PacketConnectedDisplayUpdate(display, display.getRegistryID()), watcher));
		}
		return true;
	}

	public static void setDisplayLocking(int registryID, boolean locked) {
		ConnectedDisplay display = ServerInfoHandler.instance().getConnectedDisplay(registryID);
		if (display != null) {
			display.isLocked.setObject(locked);
			DisplayHandler.instance().getConnections(registryID).forEach(d -> d.setLocked(locked));
		}
	}

	public int getNextAvailableID() {
		return ServerInfoHandler.instance().getNextIdentity();
	}

	/** abstract connection handler */
	public Pair<ConnectableType, Integer> getConnectionType(ILargeDisplay source, World world, BlockPos pos, EnumFacing dir, ConnectableType cableType) {
		ICableConnectable connection = CableHelper.getConnection(source, dir, CableConnectionType.NETWORK, false);
		if (connection instanceof ILargeDisplay && ((ILargeDisplay) connection).getCableFace() == source.getCableFace()) {
			return new Pair(ConnectableType.SCREEN, ((ILargeDisplay) connection).getRegistryID());
		}
		return new Pair(ConnectableType.NONE, -1);
	}

	@Override
	public void onNetworksConnected(int newID, int oldID) {
		ConnectedDisplay screen = ServerInfoHandler.instance().getConnectedDisplays().get(newID);
		ServerInfoHandler.instance().getConnectedDisplays().remove(oldID);
		markConnectedDisplayChanged(oldID, ConnectedDisplayChange.SUB_DISPLAY_CHANGED);

		if (screen != null) {
			markConnectedDisplayChanged(newID, ConnectedDisplayChange.SUB_DISPLAY_CHANGED);
		} else {
			PL2.logger.error("CONNECTED DISPLAY SCREEN SHOULD NOT BE NULL!");
		}
	}

	@Override
	public void onConnectionAdded(int registryID, ILargeDisplay added) {
		markConnectedDisplayChanged(registryID, ConnectedDisplayChange.SUB_DISPLAY_CHANGED);
	}

	@Override
	public void onConnectionRemoved(int registryID, ILargeDisplay added) {
		markConnectedDisplayChanged(registryID, ConnectedDisplayChange.SUB_DISPLAY_CHANGED);
	}

	@Override
	public void onNetworksDisconnected(List<Integer> newNetworks) {}

	public Pair<ConnectableType, Integer> getConnectionTypeFromObject(ILargeDisplay source, Object connection, EnumFacing dir, ConnectableType cableType) {
		return new Pair(ConnectableType.NONE, -1);
	}

	@Override
	public void addConnectionToNetwork(ILargeDisplay add) {
		addConnection(add);
	}

	@Override
	public void removeConnectionFromNetwork(ILargeDisplay remove) {
		removeConnection(remove);
	}

}