package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.File;
import java.util.ArrayList;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.Opener;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.ImageCalculator;
import ij.plugin.Orthogonal_Views;
import ij.plugin.PlugIn;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.process.ShortProcessor;
import utils.ArrayUtils;
import utils.ButtonMessages;
import utils.ImageUtils;
import utils.AboutBox;
import utils.MyCircleDetector;
import utils.MyConst;
import utils.MyGenericDialogGrid;
import utils.MyLine;
import utils.MyLog;
import utils.MyPlot;
import utils.MyStackUtils;
import utils.MyVersionUtils;
import utils.ReadDicom;
import utils.UtilAyv;

//=====================================================
//     Calcoli su di un volume definito da una mask
//     13 ottobre 2017 
//     By A.Duina - IW2AYV
//     Linguaggio: Java per ImageJ
//=====================================================

public class ShrinkMask implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {

		new AboutBox().about("ShrinkMask", MyVersion.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

		String path0 = Prefs.get("prefer.Shrink", "");

		OpenDialog.setDefaultDirectory(path0);
		OpenDialog od1 = new OpenDialog("SELEZIONARE LO STACK CON LA MASK DA STRIZZARE");
		String path1 = od1.getPath();
		if (path1 == null)
			return;
		Prefs.set("prefer.Shrink", path1);
		ImagePlus imp1 = UtilAyv.openImageMaximized(path1);
		ImagePlus imp2 = shrek1(imp1);
		UtilAyv.showImageMaximized2(imp2);
		MyLog.waitHere();
	}

	/**
	 * Restituisce, in un ArrayList fornito come parametro, i valori di tutti i
	 * pixel dell'immagine sorgente, in cui il valore del pixel corrispondente
	 * nella immagine mask non sia zero
	 * 
	 * @param impSingleImage
	 * @param impSingleMask
	 * @param pixList11
	 */
	public static void pixSingleVectorize(ImagePlus impSingleImage, ImagePlus impSingleMask,
			ArrayList<Float> pixList11) {

		ImageProcessor ipSingleImage = impSingleImage.getProcessor();
		ImageProcessor maskSingleImage = impSingleMask.getProcessor();
		for (int y1 = 0; y1 < impSingleImage.getHeight(); y1++) {
			for (int x1 = 0; x1 < impSingleImage.getWidth(); x1++) {
				if (maskSingleImage.getPixel(x1, y1) != 0) {
					pixList11.add((Float) ipSingleImage.getPixelValue(x1, y1));
				}
			}
		}
	}

	/**
	 * Restituisce, in un ArrayList fornito come parametro, i valori di tutti i
	 * pixel dello stack immagine sorgente, in cui il valore del pixel
	 * corrispondente nello stack immagine mask non sia zero
	 * 
	 * @param impSingleImage
	 * @param impSingleMask
	 * @param pixList
	 */

	public static void pixStackVectorize(ImagePlus impStackImage, ImagePlus impStackMask, ArrayList<Float> pixList) {

		for (int z1 = 1; z1 <= impStackImage.getImageStackSize(); z1++) {
			ImagePlus impSingleImage = MyStackUtils.imageFromStack(impStackImage, z1);
			ImagePlus impSingleMask = MyStackUtils.imageFromStack(impStackMask, z1);
			ImageProcessor ipSingleImage = impSingleImage.getProcessor();
			ImageProcessor maskSingleImage = impSingleMask.getProcessor();
			for (int y1 = 0; y1 < impSingleImage.getHeight(); y1++) {
				for (int x1 = 0; x1 < impSingleImage.getWidth(); x1++) {
					if (maskSingleImage.getPixel(x1, y1) != 0) {
						pixList.add((Float) ipSingleImage.getPixelValue(x1, y1));
					}
				}
			}
		}
	}

	public static void pixStackVectorizeString(ImagePlus impStackImage, ImagePlus impStackMask,
			ArrayList<String> pixList) {

		for (int z1 = 1; z1 <= impStackImage.getImageStackSize(); z1++) {
			ImagePlus impSingleImage = MyStackUtils.imageFromStack(impStackImage, z1);
			ImagePlus impSingleMask = MyStackUtils.imageFromStack(impStackMask, z1);
			ImageProcessor ipSingleImage = impSingleImage.getProcessor();
			ImageProcessor maskSingleImage = impSingleMask.getProcessor();
			for (int y1 = 0; y1 < impSingleImage.getHeight(); y1++) {
				for (int x1 = 0; x1 < impSingleImage.getWidth(); x1++) {
					if (maskSingleImage.getPixel(x1, y1) != 0) {
						pixList.add((String) "" + ipSingleImage.getPixelValue(x1, y1) + " " + x1 + " " + y1 + " " + z1);
					}
				}
			}
		}
	}

	/**
	 * dispone il contenuto dei pixel nella tabella in formato float [][][].
	 * Nella tabella di output le dimensioni saranno [z][x][y].
	 * 
	 * @param stack1
	 *            stack di immagini da processare
	 * @return valori dei pixel nel formato int[z][x][y], in cui a=posiz.
	 */

	public static float[][][] stackToMatrix(ImagePlus imp1) {

		ImageStack stack1 = imp1.getStack();

		int[] dimensions = new int[3];
		dimensions[0] = imp1.getHeight();
		dimensions[1] = imp1.getWidth();
		dimensions[2] = imp1.getStackSize();
		float pixValue = 0;

		float[][][] matrix = new float[dimensions[2]][dimensions[1]][dimensions[0]];
		IJ.log("dimensions z= " + dimensions[2] + " x= " + dimensions[1] + " y= " + dimensions[0]);
		IJ.log("dimensions z= " + matrix.length + " x= " + matrix[0].length + " y= " + matrix[0][0].length);

		Calibration cal8 = imp1.getCalibration();

		for (int z1 = 0; z1 < matrix.length; z1++) {
			ImageProcessor ip1 = stack1.getProcessor(z1+1);
			ip1.setCalibrationTable(cal8.getCTable());
			float[] sdata = (float[]) ip1.getPixels();
			for (int x1 = 0; x1 < matrix[0].length; x1++) {
				int offset = x1 * matrix[0].length;
				for (int y1 = 0; y1 < matrix[0][0].length; y1++) {
					pixValue = sdata[offset + y1];
					matrix[z1][x1][y1] = pixValue;
				}
			}
		}
		return (matrix);
	}

	public static ImagePlus matrixToStack(float[][][] matrix) {

		int slices = matrix.length;
		int width = matrix[0].length;
		int height = matrix[0][0].length;
		String title = "shreck";

		ImagePlus newimp = NewImage.createFloatImage(title, width, height, slices, NewImage.FILL_BLACK);
		ImageStack stack = newimp.getStack();

		for (int z1 = 0; z1 < matrix.length; z1++) {
			ImageProcessor ip = stack.getProcessor(z1+1);
			float[] pixels = (float[]) ip.getPixels();
			for (int x1 = 0; x1 < matrix[0].length; x1++) {
				int offset = x1 * matrix[0].length;
				for (int y1 = 0; y1 < matrix[0][0].length; y1++) {
					pixels[offset + y1] = (float) matrix[z1][x1][y1];
				}
			}
			ip.resetMinAndMax();
		}
		newimp.getProcessor().resetMinAndMax();
		return newimp;
	}

	public ImagePlus shrek1(ImagePlus imp1) {

		float[][][] matrix1 = stackToMatrix(imp1);
		/// scansione su y
		for (int z1 = 0; z1 < matrix1.length; z1++) {
			for (int x1 = 0; x1 < matrix1[0].length; x1++) {
				// lavoro su y
				float[] vecty = new float[matrix1[0][0].length];
				for (int y1 = 0; y1 < matrix1[0][0].length; y1++) {
					vecty[y1] = matrix1[z1][x1][y1];
				}
				float[] out1 = scanVector(vecty);
				for (int y1 = 0; y1 < matrix1[0][0].length; y1++) {
					matrix1[z1][x1][y1] = out1[y1];
				}
			}
		}
		/// scansione su x
		for (int z1 = 0; z1 < matrix1.length; z1++) {
			for (int y1 = 0; y1 < matrix1[0].length; y1++) {
				// lavoro su x
				float[] vectx = new float[matrix1[0].length];
				for (int x1 = 0; x1 < matrix1[0].length; x1++) {
					vectx[x1] = matrix1[z1][x1][y1];
				}
				float[] out1 = scanVector(vectx);
				for (int x1 = 0; x1 < matrix1[0].length; x1++) {
					matrix1[z1][x1][y1] = out1[x1];
				}
			}
		}
		ImagePlus out1 = matrixToStack(matrix1);
		return out1;
	}

	public float[] scanVector(float[] in1) {
		boolean active = false;
		float[] vect = new float[in1.length];
		for (int i1 = 0; i1 < vect.length; i1++) {
			vect[i1] = in1[i1];
		}
		for (int i1 = 0; i1 < vect.length; i1++) {
			if ((i1 == 0) || (vect[i1] == 0))
				active = true;
			if (active && vect[i1] > 0) {
				vect[i1] = 999;
				active = false;
			}
		}
		for (int i1 = vect.length-1; i1 > 0; i1--) {
			if ((i1 == vect.length-1) || (vect[i1] == 0))
				active = true;
			if (active && vect[i1] > 0) {
				vect[i1] = 999;
				active = false;
			}
		}

		return vect;
	}

}
