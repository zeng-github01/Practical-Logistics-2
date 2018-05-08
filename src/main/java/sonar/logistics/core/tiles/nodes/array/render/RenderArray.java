package sonar.logistics.core.tiles.nodes.array.render;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import sonar.core.helpers.RenderHelper;
import sonar.logistics.core.tiles.displays.info.InfoRenderHelper;
import sonar.logistics.core.tiles.nodes.array.TileArray;

import static net.minecraft.client.renderer.GlStateManager.*;

public class RenderArray extends TileEntitySpecialRenderer<TileArray> {

	@Override
	public void render(TileArray te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		RenderHelper.offsetRendering(te.getPos(), partialTicks);
		InfoRenderHelper.rotateDisplayRendering(te.getCableFace(), EnumFacing.NORTH, 0, 0);
		translate(-(0.0625*10) - 0.01, -0.0625*11, 0);
		rotate(90, -1, 0, 0);
		//translate(-9, -8.0, 0.45);
		for (int i = 0; i < te.inventory.getSizeInventory(); i++) {
			ItemStack stack = te.inventory.getStackInSlot(i);
			if (stack != null) {
				pushMatrix();
				if (i < 4) {
					translate(0, 0, i * 0.0625*2);
				} else{
					translate(0.0625*4, 0, (i - 4) * 0.0625*2);
				}
				GlStateManager.depthMask(true);
				scale(0.0625*5, 0.0625*5, 0.0625*5);
				RenderHelper.itemRender.renderItem(stack, TransformType.NONE);
				popMatrix();
			}
		}
		popMatrix();
	}

}