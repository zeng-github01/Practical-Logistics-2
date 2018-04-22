package sonar.logistics.networking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import sonar.core.helpers.FunctionHelper;
import sonar.core.helpers.ListHelper;
import sonar.core.helpers.NBTHelper.SyncType;
import sonar.core.listener.PlayerListener;
import sonar.core.utils.Pair;
import sonar.logistics.PL2;
import sonar.logistics.api.IInfoManager;
import sonar.logistics.api.displays.DisplayGSI;
import sonar.logistics.api.errors.IInfoError;
import sonar.logistics.api.errors.UUIDError;
import sonar.logistics.api.info.IInfo;
import sonar.logistics.api.info.InfoUUID;
import sonar.logistics.api.lists.types.AbstractChangeableList;
import sonar.logistics.api.lists.types.UniversalChangeableList;
import sonar.logistics.api.states.ErrorMessage;
import sonar.logistics.api.tiles.displays.ConnectedDisplay;
import sonar.logistics.api.tiles.displays.IDisplay;
import sonar.logistics.api.tiles.readers.IInfoProvider;
import sonar.logistics.api.utils.PL2AdditionType;
import sonar.logistics.api.utils.PL2RemovalType;
import sonar.logistics.api.viewers.ILogicListenable;
import sonar.logistics.api.viewers.ListenerType;
import sonar.logistics.helpers.PacketHelper;
import sonar.logistics.info.types.InfoError;
import sonar.logistics.networking.displays.ChunkViewerHandler;
import sonar.logistics.networking.displays.LocalProviderHandler;
import sonar.logistics.networking.info.InfoHelper;

public class ServerInfoHandler implements IInfoManager {

	// server side
	private int IDENTITY_COUNT; // gives unique identity to all PL2 Tiles ( which connect to networks), they can then be retrieved via their identity.
	public Map<ILogicListenable, List<Integer>> changedInfo = new HashMap<>(); // in the form of READER IDENTITY and then CHANGED INFO
	public Map<Integer, DisplayGSI> displays = new HashMap<>();
	private Map<InfoUUID, IInfo> info = new HashMap<>();
	public Map<InfoUUID, AbstractChangeableList> monitoredLists = new HashMap<>();
	public Map<Integer, ILogicListenable> identityTiles = new HashMap<>();
	public Map<Integer, ConnectedDisplay> connectedDisplays = new HashMap<>();

	public static ServerInfoHandler instance() {
		return (ServerInfoHandler) PL2.proxy.getServerManager();
	}

	public void removeAll() {
		changedInfo.clear();
		displays.clear();
		info.clear();
		monitoredLists.clear();
		identityTiles.clear();
		connectedDisplays.clear();
		IDENTITY_COUNT = 0;
	}

	public int getNextIdentity() {
		return IDENTITY_COUNT++;
	}

	public int getIdentityCount() {
		return IDENTITY_COUNT;
	}

	/** warning do not use unless reading WorldSavedData, or your world will be corrupted!!!! */
	public int setIdentityCount(int count) {
		return IDENTITY_COUNT = count;
	}

	public boolean enableEvents() {
		return !displays.isEmpty();
	}

	public IInfo getInfoFromUUID(InfoUUID uuid) {
		return info.get(uuid);
	}

	public ConnectedDisplay getConnectedDisplay(int iden) {
		return connectedDisplays.get(iden);
	}

	public void addIdentityTile(ILogicListenable logicTile, PL2AdditionType type) {
		if (identityTiles.containsValue(logicTile)) {
			return;
		}
		identityTiles.put(logicTile.getIdentity(), logicTile);
	}

	public void removeIdentityTile(ILogicListenable logicTile, PL2RemovalType type) {
		identityTiles.remove(logicTile.getIdentity());
	}

	public ILogicListenable getIdentityTile(int iden) {
		return identityTiles.get(iden);
	}

	public void addDisplay(IDisplay display) {
		DisplayGSI gsi = display.getGSI();
		if (gsi != null && gsi.getDisplay() != null && display.getCoords() != null && !displays.containsValue(gsi)) {
			validateGSI(display, gsi);
		}
	}

	public void removeDisplay(IDisplay display) {
		DisplayGSI gsi = display.getGSI();
		if (gsi != null && gsi.getDisplay() != null && display.getCoords() != null) {
			invalidateGSI(display, gsi);
		}
	}

	public void validateGSI(IDisplay display, DisplayGSI gsi) {
		if (gsi.getDisplay().getCoords() != null) {
			if (display == gsi.getDisplay().getActualDisplay()) {
				ChunkViewerHandler.instance().onDisplayAdded(gsi);
				gsi.validate();
				gsi.sendInfoContainerPacket();
			}
		}
	}

	public void invalidateGSI(IDisplay display, DisplayGSI gsi) {
		ChunkViewerHandler.instance().onDisplayRemoved(gsi);
		gsi.invalidate();
	}

	@Override
	public DisplayGSI getGSI(int iden) {
		return displays.get(iden);
	}

	@Nullable
	public Pair<InfoUUID, UniversalChangeableList<?>> getMonitorFromServer(InfoUUID uuid) {
		AbstractChangeableList list = getMonitoredList(uuid);
		return list != null ? new Pair(uuid, list) : null;
	}

	@Nullable
	public <T extends IInfo> AbstractChangeableList getMonitoredList(InfoUUID uuid) {
		return monitoredLists.get(uuid);
	}

	public void sendInfoUpdates() {
		if (!changedInfo.isEmpty() && !displays.isEmpty()) {
			Map<EntityPlayerMP, NBTTagList> savePackets = new HashMap<EntityPlayerMP, NBTTagList>();
			for (Entry<ILogicListenable, List<Integer>> id : changedInfo.entrySet()) {
				PL2ListenerList list = id.getKey().getListenerList();
				List<PlayerListener> listeners = list.getAllListeners(ListenerType.OLD_GUI_LISTENER, ListenerType.OLD_DISPLAY_LISTENER);
				if (!listeners.isEmpty()) {
					for (Integer i : id.getValue()) {
						InfoUUID uuid = new InfoUUID(id.getKey().getIdentity(), i);
						IInfo monitorInfo = getInfoFromUUID(uuid);
						if (monitorInfo != null) {
							NBTTagCompound saveTag = InfoHelper.writeInfoToNBT(new NBTTagCompound(), monitorInfo, SyncType.SAVE);
							if (!saveTag.hasNoTags()) {
								saveTag = uuid.writeData(saveTag, SyncType.SAVE);
								PacketHelper.createInfoUpdatesForListeners(savePackets, listeners, saveTag, saveTag, true);
							}
						}
					}
				}
			}
			if (!savePackets.isEmpty()) {// || !syncPackets.isEmpty()) {
				savePackets.entrySet().forEach(entry -> PacketHelper.sendInfoUpdatePacket(entry.getKey(), entry.getValue(), SyncType.SAVE));
				changedInfo.clear();
			}
		}
		return;

	}

	public void changeInfo(ILogicListenable source, InfoUUID id, IInfo info) {
		if (!InfoUUID.valid(id)) {
			return;
		}
		IInfo last = getInfoFromUUID(id);
		if (info == null && last != null) {
			info = InfoError.noData;
			return;
		}
		if (InfoHelper.hasInfoChanged(last, info)) {
			setInfo(id, info);
			info.onInfoStored();
			markChanged(source, id);
		}
	}

	public void markChanged(ILogicListenable source, InfoUUID... infoUUIDs) {
		for (InfoUUID id : infoUUIDs) {
			List<Integer> changes = changedInfo.computeIfAbsent(source, FunctionHelper.ARRAY);
			if (!changes.contains(id.channelID)) {
				changes.add(id.channelID);
			}
		}
	}

	@Override
	public Map<Integer, ILogicListenable> getMonitors() {
		return identityTiles;
	}

	@Override
	public Map<InfoUUID, IInfo> getInfoList() {
		return info;
	}

	@Override
	public Map<Integer, ConnectedDisplay> getConnectedDisplays() {
		return connectedDisplays;
	}

	@Override
	public void setInfo(InfoUUID uuid, IInfo newInfo) {
		info.put(uuid, newInfo);
	}

	public List<EntityPlayerMP> getPlayersWatchingUUID(InfoUUID uuid) {
		List<EntityPlayerMP> players = new ArrayList<>();

		this.displays.values().forEach(gsi -> {
			if (gsi.isDisplayingUUID(uuid)) {
				ListHelper.addWithCheck(players, ChunkViewerHandler.instance().getWatchingPlayers(gsi));
			}
		});
		return players;
	}

	public void forEachGSIDisplayingUUID(InfoUUID uuid, Consumer<DisplayGSI> action) {
		this.displays.values().forEach(gsi -> {
			if (gsi.isDisplayingUUID(uuid)) {
				action.accept(gsi);
			}
		});
	}

	//// ERROR HANDLING \\\\

	private List<IInfoError> added_errors = new ArrayList<>();

	public void addError(IInfoError error) {
		added_errors.add(error);
	}

	public void addErrors(List<IInfoError> errors) {
		added_errors.addAll(errors);
	}

	public void sendErrors() {
		if (added_errors.isEmpty()) {
			return;
		}
		Map<DisplayGSI, List<IInfoError>> affectedGSIs = new HashMap<>();
		for (IInfoError error : added_errors) {
			for (DisplayGSI gsi : displays.values()) {
				UUID: for (InfoUUID uuid : error.getAffectedUUIDs()) {
					if (gsi.isDisplayingUUID(uuid)) {
						affectedGSIs.computeIfAbsent(gsi, FunctionHelper.ARRAY);
						affectedGSIs.get(gsi).add(error);
						break UUID;
					}
				}
			}
		}
		for (Entry<DisplayGSI, List<IInfoError>> entry : affectedGSIs.entrySet()) {
			entry.getKey().addInfoErrors(entry.getValue());
			entry.getKey().sendInfoContainerPacket();
		}

	}
}