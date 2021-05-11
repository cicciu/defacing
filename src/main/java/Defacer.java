import java.util.Random;
import org.dcm4che3.data.Attributes;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
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
    PlanarImage defaceImage = defaceImage(srcImg, faceDetectImg);
    PlanarImage imageForVisualizing = rescaleForVisualizing(defaceImage, 50.0, 50.0);
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

  public static PlanarImage defaceImage(PlanarImage srcImg, PlanarImage faceDetectImg) {
    int marge = 5;

    // DRAW A LINE WHEN FACE DETECTED
    ImageCV imageDefaced = new ImageCV();
    srcImg.toMat().copyTo(imageDefaced);
    // scan the image from left to right until the face is detected in Y
    for (int x = 0; x < faceDetectImg.width(); x++) {
      for (int y = 0; y < faceDetectImg.height(); y++) {
        double faceDetectPixelValue = faceDetectImg.toMat().get(y, x)[0];
        if(faceDetectPixelValue != 0.0) {
          Point facePointDetected = new Point(x,y);
          double sourcePixelValue = 2000;
          Imgproc.line(imageDefaced, facePointDetected, new Point(x,0), new Scalar(sourcePixelValue));
        }
      }
    }
    // BLUR THIS IMAGE
    ImageCV imgBlur = new ImageCV();
    imageDefaced.toMat().copyTo(imgBlur);
    Imgproc.blur(imgBlur.toImageCV(), imgBlur.toMat(), new Size(20,20), new Point(-20, -20), Core.BORDER_DEFAULT);



    // APPLY IN THE REAL IMAGE A LINE WITH RANDOM Y OF BLUR IMAGE
    for (int x = 0; x < faceDetectImg.width(); x++) {
      for (int y = 0; y < faceDetectImg.height(); y++) {
        double faceDetectPixelValue = faceDetectImg.toMat().get(y, x)[0];
        if(faceDetectPixelValue != 0.0) {
          int yRand = DefacingUtil.randomY(faceDetectImg.height(), y, marge);
          Point facePointDetected = new Point(x,yRand);
          double sourcePixelValue = DefacingUtil.randomY(faceDetectImg.height(), y, 5);
          Scalar scalar = new Scalar(imgBlur.toMat().get(yRand, x)[0]);
          Imgproc.line(imageDefaced, facePointDetected, new Point(x,0), scalar);
        }
      }
    }

    return imageDefaced;
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
