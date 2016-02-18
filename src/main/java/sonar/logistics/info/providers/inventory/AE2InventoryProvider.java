package sonar.logistics.info.providers.inventory;

import java.util.List;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import sonar.core.inventory.StoredItemStack;
import sonar.core.utils.ActionType;
import sonar.logistics.api.LogisticsAPI;
import sonar.logistics.api.providers.InventoryHandler;
import sonar.logistics.integration.AE2Helper;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.tiles.ITileStorageMonitorable;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.MachineSource;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import cpw.mods.fml.common.Loader;

public class AE2InventoryProvider extends InventoryHandler {

	public static String name = "AE2-Inventory";

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean canHandleItems(TileEntity tile, ForgeDirection dir) {
		return tile instanceof ITileStorageMonitorable && tile instanceof IActionHost;
	}

	@Override
	public StoredItemStack getStack(int slot, TileEntity tile, ForgeDirection dir) {
		IItemList<IAEItemStack> items = getItemList(tile, dir);
		if (items == null) {
			return null;
		}
		int current = 0;
		for (IAEItemStack item : items) {
			if (current == slot) {
				return AE2Helper.convertAEItemStack(item);
			}
			current++;
		}
		return null;
	}

	@Override
	public boolean getItems(List<StoredItemStack> storedStacks, TileEntity tile, ForgeDirection dir) {
		IItemList<IAEItemStack> items = getItemList(tile, dir);
		if (items == null) {
			return false;
		}
		for (IAEItemStack item : items) {
			LogisticsAPI.getItemHelper().addStackToList(storedStacks, AE2Helper.convertAEItemStack(item));
		}
		return true;
	}

	public IItemList<IAEItemStack> getItemList(TileEntity tile, ForgeDirection dir) {
		IStorageMonitorable monitor = ((ITileStorageMonitorable) tile).getMonitorable(dir, new MachineSource(((IActionHost) tile)));
		if (monitor != null) {
			IMEMonitor<IAEItemStack> stacks = monitor.getItemInventory();
			IItemList<IAEItemStack> items = stacks.getAvailableItems(AEApi.instance().storage().createItemList());
			return items;
		}
		return null;
	}

	public boolean isLoadable() {
		return Loader.isModLoaded("appliedenergistics2");
	}

	@Override
	public StoredItemStack addStack(StoredItemStack add, TileEntity tile, ForgeDirection dir, ActionType action) {
		IStorageMonitorable monitor = ((ITileStorageMonitorable) tile).getMonitorable(dir, new MachineSource(((IActionHost) tile)));
		if (monitor != null) {
			IMEMonitor<IAEItemStack> stacks = monitor.getItemInventory();
			IAEItemStack stack = stacks.injectItems(AE2Helper.convertStoredItemStack(add), AE2Helper.getActionable(action), new MachineSource(((IActionHost) tile)));
			if (stack == null || stack.getStackSize() == 0) {
				return null;
			}
			return AE2Helper.convertAEItemStack(stack);
		}
		return add;
	}

	@Override
	public StoredItemStack removeStack(StoredItemStack remove, TileEntity tile, ForgeDirection dir, ActionType action) {
		IStorageMonitorable monitor = ((ITileStorageMonitorable) tile).getMonitorable(dir, new MachineSource(((IActionHost) tile)));
		if (monitor != null) {
			IMEMonitor<IAEItemStack> stacks = monitor.getItemInventory();
			IAEItemStack stack = stacks.extractItems(AEApi.instance().storage().createItemStack(remove.item).setStackSize(remove.stored), AE2Helper.getActionable(action), new MachineSource(((IActionHost) tile)));
			if (stack == null || stack.getStackSize() == 0) {
				return remove;
			}
			return new StoredItemStack(stack.getItemStack(), remove.stored - stack.getStackSize());
		}
		return remove;
	}

}
