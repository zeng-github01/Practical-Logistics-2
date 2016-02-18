package sonar.logistics.common.handlers;

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import sonar.core.SonarCore;
import sonar.core.fluid.StoredFluidStack;
import sonar.core.integration.fmp.FMPHelper;
import sonar.core.integration.fmp.handlers.TileHandler;
import sonar.core.inventory.StoredItemStack;
import sonar.core.network.utils.IByteBufTile;
import sonar.core.utils.BlockCoords;
import sonar.core.utils.BlockInteraction;
import sonar.core.utils.BlockInteractionType;
import sonar.core.utils.helpers.NBTHelper.SyncType;
import sonar.logistics.Logistics;
import sonar.logistics.api.Info;
import sonar.logistics.api.LogisticsAPI;
import sonar.logistics.api.StandardInfo;
import sonar.logistics.api.connecting.IInfoEmitter;
import sonar.logistics.api.connecting.IInfoReader;
import sonar.logistics.api.connecting.ILargeDisplay;
import sonar.logistics.api.interaction.IDefaultInteraction;
import sonar.logistics.api.render.InfoInteractionHandler;
import sonar.logistics.api.render.ScreenType;
import sonar.logistics.helpers.DisplayHelper;
import sonar.logistics.info.types.FluidInventoryInfo;
import sonar.logistics.info.types.FluidStackInfo;
import sonar.logistics.info.types.InventoryInfo;
import sonar.logistics.info.types.StoredStackInfo;
import sonar.logistics.registries.DisplayRegistry;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class DisplayScreenHandler extends TileHandler implements IByteBufTile {

	public Info info;
	public Info updateInfo;

	public int updateTicks, updateTime = 20;

	private long lastClickTime;
	private UUID lastClickUUID;

	public DisplayScreenHandler(boolean isMultipart, TileEntity tile) {
		super(isMultipart, tile);
	}

	@Override
	public void update(TileEntity te) {
		if (te.getWorldObj().isRemote) {
			return;
		}
		this.updateData(te, te, ForgeDirection.getOrientation(FMPHelper.getMeta(te)));

		if (updateTicks == updateTime) {
			updateTicks = 0;
			SonarCore.sendPacketAround(te, 64, 0);
		} else
			updateTicks++;
	}

	public void updateData(TileEntity te, TileEntity packetTile, ForgeDirection dir) {
		List<BlockCoords> connections = LogisticsAPI.getCableHelper().getConnections(te, dir.getOpposite());
		if (!connections.isEmpty() && connections.get(0) != null) {
			Object target = FMPHelper.getTile(connections.get(0).getTileEntity());
			if (target == null) {
				return;
			}
			Info current = null;
			boolean shouldUpdate = true;
			if (target instanceof IInfoReader) {
				IInfoReader infoReader = (IInfoReader) target;
				if (infoReader.currentInfo() != null && infoReader.getSecondaryInfo() != null) {
					Info progress = LogisticsAPI.getInfoHelper().combineData(infoReader.currentInfo(), infoReader.getSecondaryInfo());
					if (!progress.equals(info) || (info != null && info instanceof StandardInfo && progress instanceof StandardInfo && !((StandardInfo) progress).data.equals(((StandardInfo) info).data))) {
						current = progress;
					} else {
						shouldUpdate = false;
					}
				} else if (infoReader.currentInfo() != null) {
					if (!infoReader.currentInfo().equals(info) || (info != null && info instanceof StandardInfo && infoReader.currentInfo() instanceof StandardInfo && !((StandardInfo) infoReader.currentInfo()).data.equals(((StandardInfo) info).data))) {
						current = infoReader.currentInfo();
					} else {
						shouldUpdate = false;
					}
				}

			} else if (target instanceof IInfoEmitter) {
				IInfoEmitter infoNode = (IInfoEmitter) target;
				if (infoNode.currentInfo() != null) {
					if (!infoNode.currentInfo().equals(info) || (info != null && info instanceof StandardInfo && infoNode.currentInfo() instanceof StandardInfo && !((StandardInfo) infoNode.currentInfo()).data.equals(((StandardInfo) info).data))) {
						current = infoNode.currentInfo();
					} else {
						shouldUpdate = false;
					}
				}
			}
			updateInfo = current;
			if (shouldUpdate) {
				if (info == null) {
					if (updateInfo != null) {
						info = updateInfo;
						SonarCore.sendPacketAround(packetTile, 64, 0);
					}
				} else {
					if (updateInfo != null) {
						if (updateInfo.areTypesEqual(info)) {
							if (updateInfo instanceof StandardInfo) {
								info = updateInfo;
								SonarCore.sendPacketAround(packetTile, 64, 0);
							} else {
								SonarCore.sendPacketAround(packetTile, 64, 2);
							}
						} else {
							info = updateInfo;
							SonarCore.sendPacketAround(packetTile, 64, 0);
						}
					} else {
						info = null;
						SonarCore.sendPacketAround(packetTile, 64, 0);
					}
				}

			}
		}
	}

	public void screenClicked(World world, EntityPlayer player, int x, int y, int z, BlockInteraction interact) {
		TileEntity te = world.getTileEntity(x, y, z);
		if (interact.side != FMPHelper.getMeta(te)) {
			return;
		}
		boolean doubleClick = false;
		if (world.getTotalWorldTime() - lastClickTime < 10 && player.getPersistentID().equals(lastClickUUID)) {
			doubleClick = true;
		}
		lastClickTime = world.getTotalWorldTime();
		lastClickUUID = player.getPersistentID();
		TileEntity connectTe = te;
		Info screenInfo = info;
		if (te instanceof ILargeDisplay) {
			List<BlockCoords> displays = DisplayRegistry.getScreens(((ILargeDisplay) te).registryID());
			if (!displays.isEmpty()) {
				te = displays.get(0).getTileEntity();
				TileHandler tilehandler = FMPHelper.getHandler(te);
				if (tilehandler != null && tilehandler instanceof LargeDisplayScreenHandler) {
					LargeDisplayScreenHandler handlerDisplay = (LargeDisplayScreenHandler) tilehandler;
					if (handlerDisplay.connectedTile != null) {							
						connectTe = handlerDisplay.connectedTile.getTileEntity();							
						screenInfo = handlerDisplay.info;
					}
				}
			}
		}
		List<BlockCoords> connections = LogisticsAPI.getCableHelper().getConnections(connectTe, ForgeDirection.getOrientation(FMPHelper.getMeta(connectTe)).getOpposite());
		if (!connections.isEmpty() && connections.get(0) != null) {
			TileEntity readerTile = connections.get(0).getTileEntity();		
				if (readerTile == null) {
					return;
				}
				ScreenType screenType = ScreenType.NORMAL;
				if (te instanceof ILargeDisplay) {
					screenType = ScreenType.LARGE;
					if(((ILargeDisplay) te).getSizing()!=null){
						screenType = ScreenType.CONNECTED;
					}						
				}
				InfoInteractionHandler handler = Logistics.infoInteraction.getInteractionHandler(screenInfo, screenType, te, readerTile);	
				if (handler != null) {
					handler.handleInteraction(screenInfo, screenType, te, readerTile, player, x, y, z, interact, doubleClick);
				} else {
					Object reader = FMPHelper.getHandler(readerTile);
					if (reader != null && reader instanceof IDefaultInteraction) {
						IDefaultInteraction interaction = (IDefaultInteraction) reader;
						interaction.handleInteraction(screenInfo, screenType, te, readerTile, player, x, y, z, interact, doubleClick);
					}
				}
			}
			/*
			 * if (target instanceof InventoryReaderHandler) {
			 * InventoryReaderHandler handler = (InventoryReaderHandler) target;
			 * if (side == ForgeDirection.getOrientation(FMPHelper.getMeta(te)))
			 * { if (screenInfo instanceof StoredStackInfo) { StoredStackInfo
			 * storedInfo = (StoredStackInfo) screenInfo; if (interact ==
			 * BlockInteractionType.RIGHT) { if (player.getHeldItem() != null &&
			 * storedInfo.stack.equalStack(player.getHeldItem())) { if
			 * (!doubleClick) { handler.insertItem(player, readerTile,
			 * player.inventory.currentItem); } else {
			 * handler.insertInventory(player, readerTile,
			 * player.inventory.currentItem); }
			 * 
			 * } } else if (interact != BlockInteractionType.SHIFT_RIGHT) {
			 * StoredItemStack extract = handler.extractItem(readerTile,
			 * storedInfo.stack, interact == BlockInteractionType.LEFT ? 1 :
			 * 64); if (extract != null) {
			 * LogisticsAPI.getItemHelper().spawnStoredItemStack(extract, world,
			 * x, y, z, side); } } } else if (screenInfo instanceof
			 * InventoryInfo) { InventoryInfo invInfo = (InventoryInfo)
			 * screenInfo; if (interact == BlockInteractionType.RIGHT ||
			 * interact == BlockInteractionType.SHIFT_RIGHT) { if (interact ==
			 * BlockInteractionType.RIGHT) { if (player.getHeldItem() != null) {
			 * if (!doubleClick) { handler.insertItem(player, readerTile,
			 * player.inventory.currentItem); } else {
			 * handler.insertInventory(player, readerTile,
			 * player.inventory.currentItem); } } } else if (interact ==
			 * BlockInteractionType.SHIFT_RIGHT) { } } else if (teHandler
			 * instanceof LargeDisplayScreenHandler) { LargeDisplayScreenHandler
			 * largeScreen = (LargeDisplayScreenHandler) teHandler; if
			 * (largeScreen.sizing != null) { int slot = -1; if (side ==
			 * ForgeDirection.NORTH) { int hSlots =
			 * (Math.round(largeScreen.sizing.maxH - largeScreen.sizing.minH) *
			 * 2); int yPos = (largeScreen.sizing.maxY - (y -
			 * handlerCoords.getY())) * 2; int hPos = (largeScreen.sizing.maxH -
			 * (x - handlerCoords.getX())) * 2; int hSlot = hitx < 0.5 ? hPos +
			 * 1 : hPos; int ySlot = hity < 0.5 ? yPos + 1 : yPos; slot =
			 * ((ySlot * hSlots) + hSlot) + (ySlot * 2); } if (side ==
			 * ForgeDirection.SOUTH) { int hSlots =
			 * (Math.round(largeScreen.sizing.maxH - largeScreen.sizing.minH) *
			 * 2); int yPos = (largeScreen.sizing.maxY - (y -
			 * handlerCoords.getY())) * 2; int hPos = (largeScreen.sizing.maxH -
			 * largeScreen.sizing.minH + (x - handlerCoords.getX())) * 2; int
			 * hSlot = hitx < 0.5 ? hPos : hPos + 1; int ySlot = hity < 0.5 ?
			 * yPos + 1 : yPos; slot = ((ySlot * hSlots) + hSlot) + (ySlot * 2)
			 * - largeScreen.sizing.maxH * 2; } if (side == ForgeDirection.EAST)
			 * { int hSlots = (Math.round(largeScreen.sizing.maxH -
			 * largeScreen.sizing.minH) * 2); int yPos =
			 * (largeScreen.sizing.maxY - (y - handlerCoords.getY())) * 2; int
			 * hPos = (largeScreen.sizing.maxH - (z - handlerCoords.getZ())) *
			 * 2; int hSlot = hitz < 0.5 ? hPos + 1 : hPos; int ySlot = hity <
			 * 0.5 ? yPos + 1 : yPos; slot = ((ySlot * hSlots) + hSlot) + (ySlot
			 * * 2); } if (side == ForgeDirection.WEST) { int hSlots =
			 * (Math.round(largeScreen.sizing.maxH - largeScreen.sizing.minH) *
			 * 2); int yPos = (largeScreen.sizing.maxY - (y -
			 * handlerCoords.getY())) * 2; int hPos = (largeScreen.sizing.maxH -
			 * largeScreen.sizing.minH + (z - handlerCoords.getZ())) * 2; int
			 * hSlot = hitz < 0.5 ? hPos : hPos + 1; int ySlot = hity < 0.5 ?
			 * yPos + 1 : yPos; slot = ((ySlot * hSlots) + hSlot) + (ySlot * 2)
			 * - largeScreen.sizing.maxH * 2; } if (slot != -1 && slot <
			 * invInfo.stacks.size()) { StoredItemStack extract =
			 * handler.extractItem(readerTile, invInfo.stacks.get(slot),
			 * interact == BlockInteractionType.LEFT ? 1 : 64); if (extract !=
			 * null) {
			 * LogisticsAPI.getItemHelper().spawnStoredItemStack(extract, world,
			 * x, y, z, side); } }
			 * 
			 * } } } else if (player.getHeldItem() != null) { if (!doubleClick)
			 * { handler.insertItem(player, readerTile,
			 * player.inventory.currentItem); } else {
			 * handler.insertInventory(player, readerTile,
			 * player.inventory.currentItem); } }
			 * 
			 * } } if (target instanceof FluidReaderHandler) {
			 * FluidReaderHandler handler = (FluidReaderHandler) target; if
			 * (screenInfo instanceof FluidStackInfo) { FluidStackInfo info =
			 * (FluidStackInfo) screenInfo; if (interact ==
			 * BlockInteractionType.RIGHT) { handler.emptyFluid(player,
			 * readerTile, player.getHeldItem()); } else if (interact ==
			 * BlockInteractionType.LEFT) { handler.fillItemStack(player,
			 * readerTile, info.stack.setStackSize(1000)); } else if (interact
			 * == BlockInteractionType.SHIFT_LEFT) {
			 * handler.fillItemStack(player, readerTile, info.stack); } } else
			 * if (screenInfo instanceof FluidInventoryInfo) {
			 * FluidInventoryInfo info = (FluidInventoryInfo) screenInfo;
			 * 
			 * if (interact == BlockInteractionType.RIGHT) {
			 * handler.emptyFluid(player, readerTile, player.getHeldItem()); }
			 * else {
			 * 
			 * } if (teHandler instanceof LargeDisplayScreenHandler) {
			 * LargeDisplayScreenHandler largeScreen =
			 * (LargeDisplayScreenHandler) teHandler; if (largeScreen.sizing !=
			 * null) { int slot = -1; if (side == ForgeDirection.NORTH) { int
			 * hSlots = (Math.round(largeScreen.sizing.maxH -
			 * largeScreen.sizing.minH)); int yPos = (largeScreen.sizing.maxY -
			 * (y - handlerCoords.getY())); int hPos = (largeScreen.sizing.maxH
			 * - (x - handlerCoords.getX())); slot = ((yPos * hSlots) + hPos) +
			 * (yPos); } if (side == ForgeDirection.SOUTH) { int hSlots =
			 * (Math.round(largeScreen.sizing.maxH - largeScreen.sizing.minH));
			 * int yPos = (largeScreen.sizing.maxY - (y -
			 * handlerCoords.getY())); int hPos = (largeScreen.sizing.maxH -
			 * largeScreen.sizing.minH + (x - handlerCoords.getX())); slot =
			 * ((yPos * hSlots) + hPos) + (yPos) - largeScreen.sizing.maxH; } if
			 * (side == ForgeDirection.EAST) { int hSlots =
			 * (Math.round(largeScreen.sizing.maxH - largeScreen.sizing.minH));
			 * int yPos = (largeScreen.sizing.maxY - (y -
			 * handlerCoords.getY())); int hPos = (largeScreen.sizing.maxH - (z
			 * - handlerCoords.getZ())); slot = ((yPos * hSlots) + hPos) +
			 * (yPos); } if (side == ForgeDirection.WEST) { int hSlots =
			 * (Math.round(largeScreen.sizing.maxH - largeScreen.sizing.minH));
			 * int yPos = (largeScreen.sizing.maxY - (y -
			 * handlerCoords.getY())); int hPos = (largeScreen.sizing.maxH -
			 * largeScreen.sizing.minH + (z - handlerCoords.getZ())); slot =
			 * ((yPos * hSlots) + hPos) + (yPos) - largeScreen.sizing.maxH; } if
			 * (slot != -1 && slot < info.stacks.size()) { StoredFluidStack
			 * stack = info.stacks.get(slot); if (stack != null) { if (interact
			 * == BlockInteractionType.LEFT) { handler.fillItemStack(player,
			 * readerTile, stack.setStackSize(1000)); } else if (interact ==
			 * BlockInteractionType.SHIFT_LEFT) { handler.fillItemStack(player,
			 * readerTile, stack); } }
			 * 
			 * }
			 * 
			 * } } } }
			 */
		
	}

	public Info currentInfo() {
		return info;
	}

	public boolean canConnect(TileEntity te, ForgeDirection dir) {
		return dir.equals(ForgeDirection.getOrientation(FMPHelper.getMeta(te)).getOpposite());
	}

	@SideOnly(Side.CLIENT)
	public List<String> getWailaInfo(List<String> currenttip) {
		if (info != null) {
			currenttip.add("Current Data: " + info.getDisplayableData());
		}
		return currenttip;
	}

	@Override
	public void writePacket(ByteBuf buf, int id) {
		if (id == 0) {
			if (info != null) {
				buf.writeBoolean(true);
				Logistics.infoTypes.writeToBuf(buf, info);
			} else {
				buf.writeBoolean(false);
			}
		}
		if (id == 1) {
			ByteBufUtils.writeUTF8String(buf, info.getData());
		}
		if (id == 2) {
			NBTTagCompound tag = new NBTTagCompound();
			if (updateInfo != null && updateInfo.areTypesEqual(info)) {
				info.writeUpdate(updateInfo, tag);
			}
			if (!tag.hasNoTags()) {
				buf.writeBoolean(true);
				ByteBufUtils.writeTag(buf, tag);
			} else {
				buf.writeBoolean(false);
			}

		}
	}

	@Override
	public void readPacket(ByteBuf buf, int id) {
		if (id == 0) {
			if (buf.readBoolean()) {
				info = Logistics.infoTypes.readFromBuf(buf);
			} else {
				info = null;
			}
		}
		if (id == 1) {
			StandardInfo standardInfo = (StandardInfo) info;
			standardInfo.setData(ByteBufUtils.readUTF8String(buf));
		}
		if (id == 2) {
			if (buf.readBoolean()) {
				NBTTagCompound tag = ByteBufUtils.readTag(buf);
				if (tag != null && info != null)
					info.readUpdate(tag);
			}
		}
	}

	public void readData(NBTTagCompound nbt, SyncType type) {
		super.readData(nbt, type);
		if (type == SyncType.SAVE) {
			if (nbt.hasKey("currentInfo")) {
				info = Logistics.infoTypes.readFromNBT(nbt.getCompoundTag("currentInfo"));
			}
		}
	}

	public void writeData(NBTTagCompound nbt, SyncType type) {
		super.writeData(nbt, type);
		if (type == SyncType.SAVE) {
			if (info != null) {
				NBTTagCompound infoTag = new NBTTagCompound();
				Logistics.infoTypes.writeToNBT(infoTag, info);
				nbt.setTag("currentInfo", infoTag);
			}
		}
	}
}
