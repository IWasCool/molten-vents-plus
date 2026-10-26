package com.ourcreation.moltenventsplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.vent.VentFluids;
import com.ourcreation.moltenventsplus.vent.VentTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Keep independent rock/core faces and resolve the selected superheated fluid sprite. */
@EventBusSubscriber(modid = MoltenVentsPlus.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VentModels {
    private static final ResourceLocation SUPERHEATED = ResourceLocation.fromNamespaceAndPath(
            MoltenVentsPlus.MODID, "block/superheated_lava_still");

    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        models.replaceAll((id, model) -> id.id().getNamespace().equals(MoltenVentsPlus.MODID)
                && (id.id().getPath().startsWith("active_superheated_") || id.id().getPath().startsWith("dormant_superheated_"))
                ? new Core(model) : model);
    }

    private static final class Core extends BakedModelWrapper<BakedModel> {
        private final Map<ResourceLocation, Map<BakedQuad, BakedQuad>> replacements = new ConcurrentHashMap<>();
        private Core(BakedModel model) {
            super(model);
        }

        private List<BakedQuad> core(List<BakedQuad> quads) {
            var texture = VentFluids.primary(VentTier.SUPERHEATED)
                    .map(fluid -> IClientFluidTypeExtensions.of(fluid).getStillTexture()).orElse(SUPERHEATED);
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            var cache = replacements.computeIfAbsent(texture, key -> new ConcurrentHashMap<>());
            return quads.stream().map(quad -> !quad.isTinted()
                    ? quad : cache.computeIfAbsent(quad, key -> retexture(key, sprite))).toList();
        }

        private static BakedQuad retexture(BakedQuad quad, TextureAtlasSprite sprite) {
            var old = quad.getSprite();
            int[] vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int uv = vertex * stride + 4;
                float u = (Float.intBitsToFloat(vertices[uv]) - old.getU0()) / (old.getU1() - old.getU0());
                float v = (Float.intBitsToFloat(vertices[uv + 1]) - old.getV0()) / (old.getV1() - old.getV0());
                vertices[uv] = Float.floatToRawIntBits(sprite.getU(u));
                vertices[uv + 1] = Float.floatToRawIntBits(sprite.getV(v));
            }
            return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
            return core(originalModel.getQuads(state, side, random));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                        ModelData data, @Nullable RenderType renderType) {
            return core(originalModel.getQuads(state, side, random, data, renderType));
        }

        @Override
        public BakedModel applyTransform(ItemDisplayContext context, PoseStack pose, boolean leftHand) {
            originalModel.applyTransform(context, pose, leftHand);
            return this;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) { return List.of(this); }
    }

    private VentModels() {}
}
