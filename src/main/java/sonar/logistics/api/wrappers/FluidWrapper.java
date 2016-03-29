package sonar.logistics.api.wrappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import sonar.core.api.ActionType;
import sonar.core.api.FluidHandler;
import sonar.core.api.StoredFluidStack;
import sonar.core.api.StoredItemStack;
import sonar.core.api.InventoryHandler.StorageSize;
import sonar.logistics.api.cache.INetworkCache;

public class FluidWrapper {

	public static class StorageFluids {

		public static final StorageFluids EMPTY = new StorageFluids(new ArrayList(), StorageSize.EMPTY);
		public ArrayList<StoredFluidStack> fluids;
		public StorageSize sizing;

		public StorageFluids(ArrayList<StoredFluidStack> items, StorageSize sizing) {
			this.fluids = items;
			this.sizing = sizing;
		}
	}

	/** used for getting the full list of Fluids on a given network
	 * @param network the {@link INetworkCache}
	 * @return list of {@link StoredFluidStack} on the network */
	public StorageFluids getFluids(INetworkCache network) {
		return StorageFluids.EMPTY;
	}

	/** used for adding Fluids to the network
	 * @param add {@link StoredFluidStack} to add
	 * @param network the {@link INetworkCache} to add to
	 * @param action should this action be simulated
	 * @return remaining {@link StoredFluidStack} (what wasn't added), can be null */
	public StoredFluidStack addFluids(StoredFluidStack add, INetworkCache network, ActionType action) {
		return add;
	}

	/** used for removing Fluids from the network
	 * @param remove {@link StoredFluidStack} to remove
	 * @param network the {@link INetworkCache} to remove from
	 * @param action should this action be simulated
	 * @return remaining {@link StoredFluidStack} (what wasn't removed), can be null */
	public StoredFluidStack removeFluids(StoredFluidStack remove, INetworkCache network, ActionType action) {
		return remove;
	}

	/** used for filling ItemStacks with the network
	 * @param container the {@link ItemStack} to try and fill
	 * @param fill the {@link StoredFluidStack} type to fill with
	 * @param network the {@link INetworkCache} to fill from
	 * @param action should this action be simulated
	 * @return the new ItemStack */
	public ItemStack fillFluidItemStack(ItemStack container, StoredFluidStack fill, INetworkCache network, ActionType action) {
		return container;
	}

	/** used for draining ItemStacks with the network
	 * @param container the {@link ItemStack} to try and drain
	 * @param network the {@link INetworkCache} to drain into
	 * @param action should this action be simulated
	 * @return the new ItemStack */
	public ItemStack drainFluidItemStack(ItemStack container, INetworkCache network, ActionType action) {
		return container;
	}

	/** fills the players current item with a specific fluid from the network
	 * @param player the player interacting
	 * @param cache the network to fill from
	 * @param toFill the {@link StoredFluidStack} to fill with */
	public void fillHeldItem(EntityPlayer player, INetworkCache cache, StoredFluidStack toFill) {}

	/** drains the players current item into the network
	 * @param player the player interacting
	 * @param cache the network to drain into */
	public void drainHeldItem(EntityPlayer player, INetworkCache cache) {}
}