import java.util.Random;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.opencv.core.Core.MinMaxLocResult;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class DefacingUtil {

  public static int randomY(int minY, int maxY, int bound) {
    return (int)Math.floor(Math.random()*(maxY-minY+bound)+minY);
  }

  public static double pickRndYPxlColor(int xInit, int minY, int maxY, PlanarImage imgToPick) {
    int yRand = DefacingUtil.randomY(minY, maxY, 1);
    int size = 4;
    double mean = 0;
    int sum = 0;
    for (int x = xInit- (size/2); x < xInit+(size/2) +1; x++) {
      for (int y = yRand; y < yRand+size +1; y++) {
        double color = imgToPick.toMat().get(y,x)[0];
        mean = mean + color;
        sum++;
      }
    }
    return mean/sum;
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

    //int hounsfield = pixel * slope + intercept;
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
