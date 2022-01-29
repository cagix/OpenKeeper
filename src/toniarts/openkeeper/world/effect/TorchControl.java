/*
 * Copyright (C) 2014-2016 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.world.effect;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.image.RGBImageFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.KwdFile;
import toniarts.openkeeper.utils.AssetUtils;

/**
 *
 * @author ArchDemon
 */
@Deprecated
public class TorchControl extends BillboardControl {

    private final static int FRAMES = 20;
    private Material material;
    private Node torch;
    private final KwdFile kwdFile;
    private final AssetManager assetManager;
    private final PointLight light;
    private final float baseLightRadius;
    private static final AssetKey<Spatial> ASSET_KEY = new AssetKey<>("TorchFlame");

    private static final Logger log = Logger.getLogger(TorchControl.class.getName());

    public TorchControl(KwdFile kwdFile, AssetManager assetManager, float angle, PointLight light) {
        this.kwdFile = kwdFile;
        this.assetManager = assetManager;
        this.light = light;
        baseLightRadius = light.getRadius();
        setAlignment(Alignment.AxialY);
    }

    @Override
    public void setSpatial(Spatial spatial) {
        torch = (Node) spatial;

        if (torch != null) {
            this.spatial = createFlame();
            if (this.spatial != null) {
                torch.attachChild(this.spatial);
                //torch.addLight(createLight());
            }
        }
    }

    private Spatial createFlame() {
        Spatial result = ((DesktopAssetManager) assetManager).getFromCache(ASSET_KEY);

        if (result == null) {
            try {
                material = createMaterial();
                material.setTexture("DiffuseMap", createTexture());

                result = new Geometry("torch flame", createMesh(0.5f, 0.5f));
                result.setMaterial(material);
                result.setQueueBucket(RenderQueue.Bucket.Translucent);
                result.move(0.14f, 0.2f, 0);

            } catch (Exception e) {
                log.log(Level.WARNING, "Can't create torch flame", e);
            }

            ((DesktopAssetManager) assetManager).addToCache(ASSET_KEY, result);
        }

        if (result != null) {
            return result.clone();
        }

        return null;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf); //To change body of generated methods, choose Tools | Templates.

        // TODO: flicker
//        Random rand = new Random();
//
//        light.setRadius(baseLightRadius + (float) Math.sin(tpf * 0.3f) * rand.nextFloat());
    }

//    private Light createLight() {
//        PointLight result = new PointLight();
//        result.setName("torch");
//
//        float intensity = kwdFile.getVariables().get(MiscType.DEFAULT_TORCH_LIGHT_INTENSITY).getValue();
//        result.setColor(new ColorRGBA(kwdFile.getVariables().get(MiscType.DEFAULT_TORCH_LIGHT_RED).getValue() * intensity / 255,
//                kwdFile.getVariables().get(MiscType.DEFAULT_TORCH_LIGHT_GREEN).getValue() * intensity / 255,
//                kwdFile.getVariables().get(MiscType.DEFAULT_TORCH_LIGHT_BLUE).getValue() * intensity / 255, 0));
//
//        result.setEnabled(true);
//        result.setRadius(kwdFile.getVariables().get(MiscType.DEFAULT_TORCH_LIGHT_RADIUS_TILES).getValue());
//
//        // float height = kwdFile.getVariables().get(MiscType.DEFAULT_TORCH_LIGHT_HEIGHT_TILES).getValue();
//        return result;
//    }

    /**
     * Creates a quad, just that this one is centered on x-axis and on y-axis
     * lifted up by the unit height
     *
     * @param width width
     * @param height height
     * @return the mesh
     */
    private Mesh createMesh(float width, float height) {
        //Vector3f pos = torch.getLocalTranslation();
        Mesh mesh = new Mesh();

        mesh.setBuffer(VertexBuffer.Type.Position, 3, new float[]{
            -width / 2f, 0, 0,
            width / 2f, 0, 0,
            width / 2f, height, 0,
            -width / 2f, height, 0
        });

        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 1,
            1, 1,
            1, 0,
            0, 0});
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, new float[]{0, 0, 1,
            0, 0, 1,
            0, 0, 1,
            0, 0, 1});
        mesh.setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2,
            0, 2, 3});

        mesh.updateBound();
        return mesh;
    }

    private Texture createTexture() throws IOException {
        String name = "ktorch";

        //float[] scales = {1f, 1f, 1f, 1f};
        //float[] offsets = new float[4];
        //RescaleOp rop = new RescaleOp(scales, offsets, null);
        // Get the first frame, the frames need to be same size
        BufferedImage img = AssetUtils.readImageFromAsset(assetManager.locateAsset(new AssetKey(ConversionUtils.getCanonicalAssetKey("Textures/" + name + "0.png"))));

        // Create image big enough to fit all the frames
        BufferedImage text = new BufferedImage(img.getWidth() * FRAMES, img.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = text.createGraphics();
        g.drawImage(makeColorTransparent(img), 0, 0, null);
        for (int x = 1; x < FRAMES; x++) {
            AssetInfo asset = assetManager.locateAsset(new AssetKey(ConversionUtils.getCanonicalAssetKey("Textures/" + name + x + ".png")));
            img = AssetUtils.readImageFromAsset(asset);
            g.drawImage(makeColorTransparent(img), img.getWidth() * x, 0, null);
        }
        g.dispose();

        // Convert the new image to a texture
        AWTLoader loader = new AWTLoader();
        Texture result = new Texture2D(loader.load(text, false));
        return result;
    }

    private static Image makeColorTransparent(BufferedImage image) {
        ImageFilter filter = new RGBImageFilter() {
            @Override
            public final int filterRGB(int x, int y, int rgb) {
                return (rgb < 0xFF303030) ? 0x00FFFFFF : rgb;
            }
        };

        ImageProducer ip = new FilteredImageSource(image.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    private Material createMaterial() {
        Material result = new Material(assetManager, "MatDefs/LightingSprite.j3md");

        result.setInt("NumberOfTiles", FRAMES);
        result.setInt("Speed", FRAMES); // FIXME: correct value

        result.setTransparent(true);
        result.setFloat("AlphaDiscardThreshold", 0.1f);

        result.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        result.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
        //result.getAdditionalRenderState().setDepthTest(false);

        return result;
    }
}
