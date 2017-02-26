package sonar.logistics;

import java.util.ArrayList;

import mcmultipart.multipart.MultipartRegistry;
import net.minecraft.item.Item;
import sonar.core.SonarCore;
import sonar.core.registries.ISonarRegistryItem;
import sonar.core.registries.SonarRegistryItem;
import sonar.logistics.common.items.ItemDefaultMultipart;
import sonar.logistics.common.items.ItemGuide;
import sonar.logistics.common.items.ItemOperator;
import sonar.logistics.common.items.ItemScreenMultipart;
import sonar.logistics.common.items.ItemSidedMultipart;
import sonar.logistics.common.items.ItemWirelessMultipart;
import sonar.logistics.common.items.WirelessEntityTransceiver;
import sonar.logistics.common.items.WirelessItemTransceiver;
import sonar.logistics.common.multiparts.ArrayPart;
import sonar.logistics.common.multiparts.DataCablePart;
import sonar.logistics.common.multiparts.DataEmitterPart;
import sonar.logistics.common.multiparts.DataReceiverPart;
import sonar.logistics.common.multiparts.DisplayScreenPart;
import sonar.logistics.common.multiparts.EnergyReaderPart;
import sonar.logistics.common.multiparts.FluidReaderPart;
import sonar.logistics.common.multiparts.InfoReaderPart;
import sonar.logistics.common.multiparts.InventoryReaderPart;
import sonar.logistics.common.multiparts.LargeDisplayScreenPart;
import sonar.logistics.common.multiparts.NodePart;
import sonar.logistics.common.multiparts.RedstoneSignallerPart;
import sonar.logistics.common.multiparts.TransferNodePart;

public class LogisticsItems extends Logistics {

	public static ArrayList<ISonarRegistryItem> registeredItems = new ArrayList();

	public static Item register(SonarRegistryItem register) {
		Item item = register.getItem();
		item.setUnlocalizedName(register.getRegistryName());
		if (!register.ignoreNormalTab) {
			item.setCreativeTab(Logistics.creativeTab);
		}
		register.setItem(item);
		registeredItems.add(register);
		return register.getItem();
	}

	public static Item energyScreen, displayScreen, largeDisplayScreen, digitalSign, sapphire, sapphire_dust, stone_plate, transceiver, entityTransceiver, operator, guide;

	public static Item partCable, partNode, partTransferNode, partEntityNode, partArray, partRedstoneSignaller, partEmitter, partReceiver, infoReaderPart, inventoryReaderPart, fluidReaderPart, energyReaderPart;

	public static void registerItems() {
		// displayScreen = registerItem("DisplayScreenItem", new DisplayScreen());//.setTextureName(MODID + ":" + "display_screen");
		// digitalSign = registerItem("DisplayScreenItem", new DigitalSign());//.setTextureName(MODID + ":" + "digital_sign");
		guide = register(new SonarRegistryItem(new ItemGuide(), "Guide"));
		sapphire = register(new SonarRegistryItem("Sapphire"));
		sapphire_dust = register(new SonarRegistryItem("SapphireDust"));
		stone_plate = register(new SonarRegistryItem("StonePlate"));
		operator = register(new SonarRegistryItem(new ItemOperator(), "Operator"));
		
		partCable = register(new SonarRegistryItem(new ItemDefaultMultipart(DataCablePart.class), "DataCable"));
		partArray = register(new SonarRegistryItem(new ItemSidedMultipart(ArrayPart.class), "Array"));
		displayScreen = register(new SonarRegistryItem(new ItemScreenMultipart(DisplayScreenPart.class), "DisplayScreen"));
		largeDisplayScreen = register(new SonarRegistryItem(new ItemScreenMultipart(LargeDisplayScreenPart.class), "LargeDisplayScreen"));

		inventoryReaderPart = register(new SonarRegistryItem(new ItemSidedMultipart(InventoryReaderPart.class), "InventoryReader"));
		fluidReaderPart = register(new SonarRegistryItem(new ItemSidedMultipart(FluidReaderPart.class), "FluidReader"));
		partNode = register(new SonarRegistryItem(new ItemSidedMultipart(NodePart.class), "Node"));
		partTransferNode = register(new SonarRegistryItem(new ItemSidedMultipart(TransferNodePart.class), "TransferNode"));
		infoReaderPart = register(new SonarRegistryItem(new ItemSidedMultipart(InfoReaderPart.class), "InfoReader"));
		energyReaderPart = register(new SonarRegistryItem(new ItemSidedMultipart(EnergyReaderPart.class), "EnergyReader"));
		transceiver = register(new SonarRegistryItem(new WirelessItemTransceiver().setMaxStackSize(1), "Transceiver"));
		entityTransceiver = register(new SonarRegistryItem(new WirelessEntityTransceiver().setMaxStackSize(1), "EntityTransceiver"));
		partEmitter = register(new SonarRegistryItem(new ItemWirelessMultipart(DataEmitterPart.class), "DataEmitter"));
		partReceiver = register(new SonarRegistryItem(new ItemWirelessMultipart(DataReceiverPart.class), "DataReceiver"));
		partRedstoneSignaller = register(new SonarRegistryItem(new ItemSidedMultipart(RedstoneSignallerPart.class), "RedstoneSignaller"));

		MultipartRegistry.registerPart(DataCablePart.class, Logistics.MODID + ":DataCable");
		MultipartRegistry.registerPart(NodePart.class, Logistics.MODID + ":Node");
		MultipartRegistry.registerPart(TransferNodePart.class, Logistics.MODID + ":TransferNode");
		MultipartRegistry.registerPart(ArrayPart.class, Logistics.MODID + ":Array");
		MultipartRegistry.registerPart(InventoryReaderPart.class, Logistics.MODID + ":InventoryReader");
		MultipartRegistry.registerPart(FluidReaderPart.class, Logistics.MODID + ":FluidReader");
		MultipartRegistry.registerPart(InfoReaderPart.class, Logistics.MODID + ":InfoReader");
		MultipartRegistry.registerPart(DataEmitterPart.class, Logistics.MODID + ":DataEmitter");
		MultipartRegistry.registerPart(DataReceiverPart.class, Logistics.MODID + ":DataReceiver");
		MultipartRegistry.registerPart(DisplayScreenPart.class, Logistics.MODID + ":DisplayScreen");
		MultipartRegistry.registerPart(LargeDisplayScreenPart.class, Logistics.MODID + ":LargeDisplayScreen");
		MultipartRegistry.registerPart(EnergyReaderPart.class, Logistics.MODID + ":EnergyReader");
		MultipartRegistry.registerPart(RedstoneSignallerPart.class, Logistics.MODID + ":RedstoneSignaller");

		SonarCore.registerItems(registeredItems);
	}
}
