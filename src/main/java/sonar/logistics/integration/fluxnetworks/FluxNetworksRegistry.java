package sonar.logistics.integration.fluxnetworks;

import com.google.common.collect.Lists;
import sonar.fluxnetworks.api.network.AccessLevel;
//import sonar.fluxnetworks.api.network.EnergyStats;
import sonar.fluxnetworks.api.network.IFluxNetwork;
import sonar.fluxnetworks.api.tiles.IFluxConnector;
import sonar.fluxnetworks.api.network.ConnectionType;
import sonar.fluxnetworks.common.connection.NetworkStatistics;
import sonar.logistics.api.asm.ASMInfoRegistry;
import sonar.logistics.api.core.tiles.displays.info.ClientNameConstants;
import sonar.logistics.api.core.tiles.displays.info.register.IInfoRegistry;
import sonar.logistics.api.core.tiles.displays.info.register.IMasterInfoRegistry;
import sonar.logistics.api.core.tiles.displays.info.register.RegistryType;

@ASMInfoRegistry(modid = "FluxNetworks")
public class FluxNetworksRegistry implements IInfoRegistry {

	public void registerBaseReturns(IMasterInfoRegistry registry) {
		registry.registerValidReturn(IFluxNetwork.class);
		registry.registerValidReturn(ConnectionType.class);
		registry.registerValidReturn(AccessLevel.class);
		registry.registerValidReturn(NetworkStatistics.class);
//		registry.registerValidReturn(EnergyStats.class);
	}

	public void registerBaseMethods(IMasterInfoRegistry registry) {
		registry.registerMethods(IFluxConnector.class, RegistryType.TILE, Lists.newArrayList("getCoords", "getNetwork", "getConnectionType", "getMaxTransferLimit", "getRawLimit", "getRawPriority", "getCustomName"), false);
		registry.registerMethods(IFluxNetwork.class, RegistryType.TILE, Lists.newArrayList("getMemberPermission", "getNetworkID", "getNetworkName"), false);
		registry.registerMethods(AccessLevel.class, RegistryType.TILE, Lists.newArrayList("getName"), false);
//		registry.registerMethods(NetworkStatistics.class, RegistryType.TILE, Lists.newArrayList("getLatestStats"), false);
//		registry.registerMethods(EnergyStats.class, RegistryType.TILE, Lists.newArrayList("getLatestStats"), false);

	}

	public void registerAllFields(IMasterInfoRegistry registry) {
//		registry.registerFields(EnergyStats.class, RegistryType.TILE, Lists.newArrayList("transfer", "maxSent", "maxReceived"));
	}

	public void registerAdjustments(IMasterInfoRegistry registry) {
		registry.registerInfoAdjustments(Lists.newArrayList("EnergyStats.transfer", "EnergyStats.maxSent", "EnergyStats.maxReceived", "IFlux.getTransferLimit", "IFlux.getCurrentTransferLimit"), "", "RF/t");
		registry.registerInfoAdjustments(Lists.newArrayList("IFluxCommon.getEnergyAvailable", "IFluxCommon.getMaxEnergyStored"), "", "RF");
		registry.registerClientNames(ClientNameConstants.PRIORITY, Lists.newArrayList("IFlux.getCurrentPriority"));
	}
	
}
