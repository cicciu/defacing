import org.dcm4che3.data.Attributes;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class Defacer {

  public static final String APPLY_DEFACING = "defacing";

  private Defacer() {}

  public static PlanarImage apply(Attributes attributes, PlanarImage srcImg) {
    PlanarImage faceDetectImg = faceDetect(srcImg);
    PlanarImage defaceImage = addRandPxlLine(srcImg, faceDetectImg);
    PlanarImage imageForVisualizing = rescaleForVisualizing(defaceImage, 100.0, 50.0);
    return imageForVisualizing;
  }

  public static PlanarImage faceDetect(PlanarImage srcImg) {
    ImageCV faceDetectImg = new ImageCV();
    srcImg.toMat().copyTo(faceDetectImg);

    MinMaxLocResult minMaxLocResult = ImageProcessor.findMinMaxValues(faceDetectImg.toMat());

    // THRESHOLD
    Imgproc.threshold(faceDetectImg.toImageCV(), faceDetectImg.toMat(), 200, minMaxLocResult.maxVal, Imgproc.THRESH_BINARY);

    // ERODE
    Mat kernel = new Mat();
    int kernel_size = 3;
    Mat ones = Mat.ones( kernel_size, kernel_size, CvType.CV_32F);
    Core.multiply(ones, new Scalar(1/(double)(kernel_size*kernel_size)), kernel);
    Imgproc.erode(faceDetectImg.toImageCV(), faceDetectImg.toMat(), kernel);

    // FILL BLACK HOLE
    Mat kernel2 = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(30,30));
    Imgproc.morphologyEx(faceDetectImg.toImageCV(), faceDetectImg.toMat(), Imgproc.MORPH_CLOSE, kernel2);

    // RESCALE 8BIT
    faceDetectImg = transformToByte(faceDetectImg).toImageCV();

    // CANNY DETECT CONTOUR
    Imgproc.Canny(faceDetectImg.toImageCV(), faceDetectImg.toMat(), 240, 260);

    // DRAW BLACK RECT 1/3
    Rect rect = new Rect(0, faceDetectImg.height()/3, faceDetectImg.width(),faceDetectImg.height());
    Imgproc.rectangle(faceDetectImg.toImageCV(), rect, new Scalar(0,0,0), Imgproc.FILLED);

    return faceDetectImg;
  }

  public static PlanarImage addRandPxlLine(PlanarImage srcImg, PlanarImage faceDetectImg) {
    ImageCV randPxlLineImg = new ImageCV();
    srcImg.toMat().copyTo(randPxlLineImg);

    // DRAW A LINE WITH RANDOM VALUE WHEN FACE DETECTED
    int marge = 10;
    // scan the image from left to right and bottom to top until the face is detected in Y
    for (int x = 0; x < faceDetectImg.width(); x++) {
      boolean faceDetected = false;
      int yPositionFaceDetected = 0;

      for (int y = faceDetectImg.height()-1; y > 0; y--) {
        double faceDetectPixelValue = faceDetectImg.toMat().get(y, x)[0];
        if(faceDetectPixelValue != 0.0 ) {
          faceDetected = true;
          yPositionFaceDetected = y;
        }

        if(faceDetected) {
          int yRand = DefacingUtil.randomY(yPositionFaceDetected, yPositionFaceDetected+marge, 1);
          double randomPixelColor = srcImg.toMat().get(yRand, x)[0];
          randPxlLineImg.toMat().put(y ,x, randomPixelColor);
        }
      }
    }

    // BLUR THIS IMAGE
    /*ImageCV imgBlur = new ImageCV();
    randPxlLineImg.toMat().copyTo(imgBlur);
    Imgproc.blur(imgBlur.toImageCV(), imgBlur.toMat(), new Size(20,20), new Point(-20, -20), Core.BORDER_DEFAULT);*/

    return randPxlLineImg;
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

  public static PlanarImage rescaleForVisualizing(PlanarImage srcImg, Double contrast, Double brigtness) {
    ImageCV imageForVisualizing = new ImageCV();
    srcImg.toMat().copyTo(imageForVisualizing);
    PlanarImage transformImg = transformToByte(imageForVisualizing);
    transformImg = ImageProcessor.rescaleToByte(transformImg.toImageCV(), contrast / 100.0, brigtness);
    return transformImg;
  }
}
