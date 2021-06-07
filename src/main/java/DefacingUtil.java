import java.security.SecureRandom;
import java.util.Random;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.opencv.core.Core.MinMaxLocResult;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class DefacingUtil {

  private DefacingUtil() {}

  public static int randomY(int minY, int maxY, int bound) {
    SecureRandom secureRandom = new SecureRandom();
    double randomDouble = secureRandom.nextDouble();
    return (int)Math.floor(randomDouble*(maxY-minY+bound)+minY);
  }

  public static double pickRndYPxlColor(int xInit, int minY, int maxY, PlanarImage imgToPick) {
    int yRand = DefacingUtil.randomY(minY, maxY, 1);
    int size = 4;
    double mean = 0;
    int sum = 0;
    int imgWidth = imgToPick.width();
    int imgHeight = imgToPick.height();

    // convolution
    for (int x = xInit - (size / 2); x < xInit + (size / 2) + 1; x++) {
      for (int y = yRand; y < yRand + size + 1; y++) {
        int xPickColor = checkBoundsOfImageX(x, imgWidth);
        int yPickColor = checkBoundsOfImageY(y, imgHeight);
        double color = imgToPick.toMat().get(yPickColor, xPickColor)[0];

        mean = mean + color;
        sum++;
      }
    }
    if (sum != 0) {
      return mean / sum;
    }
    return mean;
  }

  public static int checkBoundsOfImageX(int x, int imgWidth) {
    if (x < 0) {
      return 0;
    }
    if (x >= imgWidth) {
      return imgWidth - 1;
    }
    return x;
  }

  public static int checkBoundsOfImageY(int y, int imgHeight) {
    if (y < 0) {
      return 0;
    }
    if (y >= imgHeight) {
      return imgHeight - 1;
    }
    return y;
  }

  public static PlanarImage transformToByte(PlanarImage srcImg) {
    ImageCV imgTransform = new ImageCV();
    srcImg.toMat().copyTo(imgTransform);

    MinMaxLocResult minMaxLocResult = ImageProcessor.findMinMaxValues(imgTransform.toMat());
    double min = minMaxLocResult.minVal;
    double max = minMaxLocResult.maxVal;
    double slope = 255.0 / (max - min);
    double yint = 255.0 - slope * max;
    imgTransform = ImageProcessor.rescaleToByte(imgTransform.toImageCV(), slope, yint);
    return imgTransform;
  }

  public static double hounsfieldToPxlValue(Attributes attributes, double hounsfield) {
    String interceptS = attributes.getString(Tag.RescaleIntercept);
    String slopeS = attributes.getString(Tag.RescaleSlope);
    double intercept = Double.parseDouble(interceptS);
    double slope = Double.parseDouble(slopeS);

    return (hounsfield - intercept) / slope;
  }

  public static PlanarImage rescaleForVisualizing(PlanarImage srcImg, Double contrast, Double brigtness) {
    ImageCV imageForVisualizing = new ImageCV();
    srcImg.toMat().copyTo(imageForVisualizing);
    PlanarImage transformImg = transformToByte(imageForVisualizing);
    transformImg = ImageProcessor.rescaleToByte(transformImg.toImageCV(), contrast / 100.0, brigtness);
    return transformImg;
  }
}
