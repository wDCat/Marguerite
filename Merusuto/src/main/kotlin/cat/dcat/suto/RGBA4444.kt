package cat.dcat.suto

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import com.sun.deploy.uitoolkit.ToolkitStore.dispose
import java.awt.Color
import java.awt.Graphics2D


/**
 * Created by DCat on 2018/1/22.
 */
fun RABGToARBG(image: BufferedImage): BufferedImage {
    val out = BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR)

    for (y in 0..image.height - 1) {
        for (x in 0..image.width - 1) {
            val c = Color(image.getRGB(x, y), true)
            var n = Color(c.alpha, c.blue, c.green, c.red).rgb
            out.setRGB(x, y, n)
        }
    }
    return out
}
