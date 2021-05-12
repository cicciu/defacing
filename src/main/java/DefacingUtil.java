import java.util.Random;

public class DefacingUtil {

  public static int randomY(int minY, int maxY, int bound) {
    return (int)Math.floor(Math.random()*(maxY-minY+bound)+minY);
  }
}
