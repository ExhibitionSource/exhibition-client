package optifine;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.lang.ref.WeakReference;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FilenameUtils;

public class CapeUtils
{
    public static void downloadCape(final AbstractClientPlayer p_downloadCape_0_)
    {
        String s = p_downloadCape_0_.getNameClear();

        if (s != null && !s.isEmpty())
        {
            String s1 = "http://s.optifine.net/capes/" + s + ".png";
            String s2 = FilenameUtils.getBaseName(s1);
            final ResourceLocation resourcelocation = new ResourceLocation("capeof/" + s2);
            TextureManager texturemanager = Minecraft.getMinecraft().getTextureManager();
            ITextureObject itextureobject = texturemanager.getTexture(resourcelocation);

            if (itextureobject != null && itextureobject instanceof ThreadDownloadImageData)
            {
                ThreadDownloadImageData threaddownloadimagedata = (ThreadDownloadImageData)itextureobject;

                if (threaddownloadimagedata.imageFound != null)
                {
                    if (threaddownloadimagedata.imageFound.booleanValue())
                    {
                        p_downloadCape_0_.setLocationOfCape(resourcelocation);
                    }

                    return;
                }
            }

            IImageBuffer iimagebuffer = new CapeImageBuffer(p_downloadCape_0_, resourcelocation);
            ThreadDownloadImageData threaddownloadimagedata1 = new ThreadDownloadImageData((File)null, s1, (ResourceLocation)null, iimagebuffer);
            threaddownloadimagedata1.pipeline = true;
            texturemanager.loadTexture(resourcelocation, threaddownloadimagedata1);
        }
    }

    public static class CapeImageBuffer implements IImageBuffer {

        public ImageBufferDownload imageBufferDownload;
        public final WeakReference<AbstractClientPlayer> playerRef;
        public final ResourceLocation resourceLocation;

        public CapeImageBuffer(AbstractClientPlayer player, ResourceLocation resourceLocation) {
            playerRef = new WeakReference<>(player);
            this.resourceLocation = resourceLocation;
            imageBufferDownload = new ImageBufferDownload();
        }

        @Override
        public BufferedImage parseUserSkin(BufferedImage image) {
            // ClassTransformer will remap to:
            // CapeUtils.parseCape(image);
            return parseCape(image);
        }

        private static BufferedImage parseCape(BufferedImage image) {
            return null;
        }

        @Override
        public void skinAvailable() {
            AbstractClientPlayer player = playerRef.get();
            if (player != null) {
                // ClassTransformer will remap to:
                // player.setLocationOfCape(resourceLocation);
                setLocationOfCape(player, resourceLocation);
            }
        }

        private static void setLocationOfCape(AbstractClientPlayer player, ResourceLocation resourceLocation) {
        }
    }

    public static BufferedImage parseCape(BufferedImage p_parseCape_0_)
    {
        int i = 64;
        int j = 32;
        int k = p_parseCape_0_.getWidth();

        for (int l = p_parseCape_0_.getHeight(); i < k || j < l; j *= 2)
        {
            i *= 2;
        }

        BufferedImage bufferedimage = new BufferedImage(i, j, 2);
        Graphics graphics = bufferedimage.getGraphics();
        graphics.drawImage(p_parseCape_0_, 0, 0, (ImageObserver)null);
        graphics.dispose();
        return bufferedimage;
    }
}
