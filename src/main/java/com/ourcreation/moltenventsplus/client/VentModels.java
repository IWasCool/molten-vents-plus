package com.ourcreation.moltenventsplus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ourcreation.moltenventsplus.MoltenVentsPlus;
import com.ourcreation.moltenventsplus.compat.VentResource;
import com.ourcreation.moltenventsplus.registry.ResourceVentRegistry;
import com.ourcreation.moltenventsplus.vent.VentFluids;
import com.ourcreation.moltenventsplus.vent.VentTier;
import com.ourcreation.moltenventsplus.vent.WeatheredVentStoneBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
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

/** Keep independent rock/core faces and resolve optional native resource and fluid sprites. */
@EventBusSubscriber(modid = MoltenVentsPlus.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VentModels {
    private static final ResourceLocation SUPERHEATED = ResourceLocation.fromNamespaceAndPath(
            MoltenVentsPlus.MODID, "block/superheated_lava_still");

    @SubscribeEvent
    public static void models(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        // Provider models are already baked. Retain vanilla fallback models if the provider is absent.
        for (var resource : VentResource.values()) {
            resource.nativeOre().ifPresent(ore -> {
                var nativeBlock = models.get(BlockModelShaper.stateToModelLocation(ore.defaultBlockState()));
                var nativeItem = models.get(new ModelResourceLocation(ore.builtInRegistryHolder().key().location(), "inventory"));
                for (boolean impure : new boolean[]{true, false}) {
                    var stone = ResourceVentRegistry.stone(resource, impure);
                    if (nativeBlock != null) for (var state : stone.getStateDefinition().getPossibleStates()) {
                        var key = BlockModelShaper.stateToModelLocation(state);
                        if (state.hasProperty(WeatheredVentStoneBlock.PROFILE)
                                && state.getValue(WeatheredVentStoneBlock.PROFILE).partial()) {
                            var detail = models.get(key);
                            if (detail != null) models.put(key, new Core(detail, nativeBlock.getParticleIcon(ModelData.EMPTY), false));
                        } else models.put(key, nativeBlock);
                    }
                    if (nativeItem != null) models.put(new ModelResourceLocation(stone.builtInRegistryHolder().key().location(), "inventory"), nativeItem);
                }
                if (nativeBlock != null) {
                    var rock = nativeBlock.getParticleIcon(ModelData.EMPTY);
                    models.replaceAll((id, model) -> id.id().getNamespace().equals(MoltenVentsPlus.MODID)
                            && (id.id().getPath().startsWith("active_") || id.id().getPath().startsWith("dormant_"))
                            && id.id().getPath().endsWith("_" + resource.id) ? new Core(model, rock, false) : model);
                }
            });
        }
        models.replaceAll((id, model) -> id.id().getNamespace().equals(MoltenVentsPlus.MODID)
                && (id.id().getPath().startsWith("active_superheated_") || id.id().getPath().startsWith("dormant_superheated_"))
                ? new Core(model, null, true) : model);
    }

    private static final class Core extends BakedModelWrapper<BakedModel> {
        private final Map<ResourceLocation, Map<BakedQuad, BakedQuad>> replacements = new ConcurrentHashMap<>();
        private final TextureAtlasSprite rock;
        private final boolean superheated;

        private Core(BakedModel model, @Nullable TextureAtlasSprite rock, boolean superheated) {
            super(model);
            this.rock = rock;
            this.superheated = superheated;
        }

        @Override
        public TextureAtlasSprite getParticleIcon(ModelData data) {
            return rock != null ? rock : originalModel.getParticleIcon(data);
        }

        private List<BakedQuad> core(List<BakedQuad> quads) {
            var texture = superheated ? VentFluids.primary(VentTier.SUPERHEATED)
                    .map(fluid -> IClientFluidTypeExtensions.of(fluid).getStillTexture()).orElse(SUPERHEATED) : rock.contents().name();
            var sprite = superheated ? Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture) : rock;
            var cache = replacements.computeIfAbsent(texture, key -> new ConcurrentHashMap<>());
            return quads.stream().map(quad -> !(superheated ? quad.isTinted() : !quad.isTinted())
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
