package uni3D;

import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

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
//     Elaborazioni Mask
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

		// String path0 = Prefs.get("prefer.Shrink", "");
		//
		// OpenDialog.setDefaultDirectory(path0);
		// OpenDialog od1 = new OpenDialog("SELEZIONARE LO STACK CON LA MASK DA
		// STRIZZARE");
		// String path1 = od1.getPath();
		// if (path1 == null)
		// return;
		// Prefs.set("prefer.Shrink", path1);

		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.noImage();
			return;
		}
		String[] titles = new String[wList.length];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null)
				titles[i] = imp.getTitle();
			else
				titles[i] = "";
		}
		GenericDialog gd = new GenericDialog("ShrinkMask");
		String defaultItem;
		String title1 = "";
		if (title1.equals(""))
			defaultItem = titles[0];
		else
			defaultItem = title1;
		gd.addChoice("Mask Stack:", titles, defaultItem);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int index1 = gd.getNextChoiceIndex();
		title1 = titles[index1];
		// operator = gd.getNextChoiceIndex();
		ImagePlus imp1 = WindowManager.getImage(wList[index1]);
		int type = imp1.getBitDepth();
		if (type != 32)
			MyLog.waitHere(
					"MESSAGE FOR THE SCREEN - KEYBOARD INTERFACE: MASK IMAGES ARE 32 BITS, INSTEAD YOUR IMAGE IS "
							+ type + " BITS");

		GenericDialog gd3 = new GenericDialog("FUNZIONAMENTO");

		String[] diciture = { "MARCA BORDO", "SBUCCIA BORDO", "ISOLA VALORE" };
		gd3.addRadioButtonGroup("SCELTA OPERAZIONE", diciture, 2, 2, null);
		gd3.showDialog();
		if (gd3.wasCanceled()) {
			return;
		}

		String sel = gd3.getNextRadioButton();
		int selnum = 999;
		for (int i1 = 0; i1 < diciture.length; i1++) {
			if (sel.equals(diciture[i1])) {
				selnum = i1;
				break;
			}
		}
		ImagePlus imp2 = null;
		boolean isola = false;
		switch (selnum) {

		case 0:
			imp2 = bordoMatrix(imp1);
			break;
		case 1:
			imp2 = sbucciaMatrix(imp1);
			break;
		case 2:
			isola = true;
			break;
		}
		if (isola) {
			IJ.log("isola");
			int[] val = singleMaskValues(imp1);

			ArrayList<Integer> intlist = new ArrayList<Integer>();
			ArrayList<String> stringlist = new ArrayList<String>();

			String[] labels = { "10 Left-Thalamus-Proper", "11 Left-Caudate", "12 Left-Putamen", "13 Left-Pallidum",
					"16 Brain-Stem /4th Ventricle", "17 Left-Hippocampus", "18 Left-Amygdala", "26 Left-Accumbens-area",
					"49 Right-Thalamus-Proper", "50 Right-Caudate", "51 Right-Putamen", "52 Right-Pallidum",
					"53 Right-Hippocampus", "54 Right-Amygdala", "58 Right-Accumbens-area" };

			int[] valuelabels = { 10, 11, 12, 13, 16, 17, 18, 26, 49, 50, 51, 52, 53, 54, 58 };

			for (int i1 = 0; i1 < valuelabels.length; i1++) {
				intlist.add(valuelabels[i1]);
				stringlist.add(labels[i1]);
			}

			for (int i1 = 0; i1 < val.length; i1++) {
				boolean tr1 = false;
				for (int i2 = 0; i2 < valuelabels.length; i2++) {
					if (val[i1] == valuelabels[i2])
						tr1 = true;
				}
				if (tr1 == false) {
					intlist.add(val[i1]);
					stringlist.add("" + val[i1]);
				}
			}

			String[] vetstring = ArrayUtils.arrayListToArrayString(stringlist);
			int[] vetint = ArrayUtils.arrayListToArrayInt(intlist);
			int vert = 4;
			int hor = (vetstring.length + vert - 1) / vert;
			int newlen = vert * hor;
			String[] labels2 = new String[newlen];
			for (int i1 = 0; i1 < vetstring.length; i1++) {
				labels2[i1] = vetstring[i1];
			}

			boolean[] defaultvalues = new boolean[newlen];
			for (int i1 = 0; i1 < val.length; i1++) {
				for (int i2 = 0; i2 < vetint.length; i2++) {
					if (val[i1] == vetint[i2])
						defaultvalues[i2] = true;
				}
				defaultvalues[0] = true;
			}

			GenericDialog gd2 = new GenericDialog("SELEZIONARE VALORI DA MANTENERE (UNO O PIU')");
			gd2.addCheckboxGroup(hor, vert, labels2, defaultvalues);
			gd2.showDialog();
			if (gd2.wasCanceled())
				return;
			Vector<Checkbox> checkboxes = gd2.getCheckboxes();
			ArrayList<Integer> arrayElimina = new ArrayList<Integer>();

			for (int i1 = 0; i1 < vetint.length; i1++) {
				if ((checkboxes.elementAt(i1).getState() == false)) {
					arrayElimina.add(vetint[i1]);
				}
			}
			int[] vetElimina = ArrayUtils.arrayListToArrayInt(arrayElimina);
			imp2 = isolaMatrix(imp1, vetElimina);
		}
		UtilAyv.showImageMaximized2(imp2);
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
		// IJ.log("dimensions z= " + dimensions[2] + " x= " + dimensions[1] + "
		// y= " + dimensions[0]);
		// IJ.log("dimensions z= " + matrix.length + " x= " + matrix[0].length +
		// " y= " + matrix[0][0].length);

		// Calibration cal8 = imp1.getCalibration();

		for (int z1 = 0; z1 < matrix.length; z1++) {
			ImageProcessor ip1 = stack1.getProcessor(z1 + 1);
			// ip1.setCalibrationTable(cal8.getCTable());
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
			ImageProcessor ip = stack.getProcessor(z1 + 1);
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

	public ImagePlus bordoMatrix(ImagePlus imp1) {

		float[][][] matrix1 = stackToMatrix(imp1);
		for (int z1 = 0; z1 < matrix1.length; z1++) {
			for (int x1 = 0; x1 < matrix1[0].length; x1++) {
				// lavoro su y
				float[] vecty = new float[matrix1[0][0].length];
				for (int y1 = 0; y1 < matrix1[0][0].length; y1++) {
					vecty[y1] = matrix1[z1][x1][y1];
				}
				float[] out1 = scanVector(vecty, 100);
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
				float[] out1 = scanVector(vectx, 100); // 200
				for (int x1 = 0; x1 < matrix1[0].length; x1++) {
					matrix1[z1][x1][y1] = out1[x1];
				}
			}
		}

		/// scansione su z
		for (int x1 = 0; x1 < matrix1[0].length; x1++) {
			for (int y1 = 0; y1 < matrix1[0].length; y1++) {
				// lavoro su x
				float[] vectz = new float[matrix1[0].length];
				for (int z1 = 0; z1 < matrix1[0].length; z1++) {
					vectz[z1] = matrix1[z1][x1][y1];
				}
				float[] out1 = scanVector(vectz, 100); // 300
				for (int z1 = 0; z1 < matrix1[0].length; z1++) {
					matrix1[z1][x1][y1] = out1[z1];
				}
			}
		}

		ImagePlus impOut = matrixToStack(matrix1);
		return impOut;
	}

	public float[] scanVector(float[] in1, int summa) {
		boolean inside = false;
		float[] vect = new float[in1.length];
		for (int i1 = 0; i1 < vect.length; i1++) {
			vect[i1] = in1[i1];
		}
		float previous = 0;
		float next = 0;
		for (int i1 = 0; i1 < vect.length; i1++) {
			if (vect[i1] != previous)
				inside = false;
			if (vect[i1] != previous && vect[i1] < 100 && vect[i1] > 0 && previous < 100 && !inside) {
				inside = true;
				vect[i1] = vect[i1] + summa;
			}
			previous = vect[i1];
		}
		inside = false;
		for (int i1 = vect.length - 1; i1 > 0; i1--) {
			if (vect[i1] != next)
				inside = false;
			if (vect[i1] != next && vect[i1] < 100 && vect[i1] > 0 && next < 100 && !inside) {
				inside = true;
				vect[i1] = vect[i1] + summa;
			}
			next = vect[i1];
		}
		return vect;
	}

	public ImagePlus sbucciaMatrix(ImagePlus imp1) {

		float[][][] matrix1 = stackToMatrix(imp1);
		for (int z1 = 0; z1 < matrix1.length; z1++) {
			for (int x1 = 0; x1 < matrix1[0].length; x1++) {
				for (int y1 = 0; y1 < matrix1[0][0].length; y1++) {
					if ((int) matrix1[z1][x1][y1] > 100)
						matrix1[z1][x1][y1] = 0;
				}
			}
		}
		ImagePlus impOut = matrixToStack(matrix1);
		return impOut;
	}

	public ImagePlus isolaMatrix(ImagePlus imp1, int[] vetElimina) {

		IJ.log("isola2");

		float[][][] matrix1 = stackToMatrix(imp1);
		for (int z1 = 0; z1 < matrix1.length; z1++) {
			for (int x1 = 0; x1 < matrix1[0].length; x1++) {
				for (int y1 = 0; y1 < matrix1[0][0].length; y1++) {
					for (int w1 = 0; w1 < vetElimina.length; w1++) {
						if ((int) matrix1[z1][x1][y1] == vetElimina[w1])
							matrix1[z1][x1][y1] = 0;
					}
				}
			}
		}
		ImagePlus impOut = matrixToStack(matrix1);
		return impOut;
	}

	public static int[] singleMaskValues(ImagePlus impStackMask) {

		ArrayList<Integer> maskList = new ArrayList<Integer>();
		for (int z1 = 1; z1 <= impStackMask.getImageStackSize(); z1++) {
			ImagePlus impSingleMask = MyStackUtils.imageFromStack(impStackMask, z1);
			ImageProcessor maskSingleImage = impSingleMask.getProcessor();
			float[] maskPixels = (float[]) maskSingleImage.getPixels();
			for (int i1 = 0; i1 < maskPixels.length; i1++) {
				int aux1 = (int) maskPixels[i1];
				boolean trovato = false;
				if (aux1 != 0) {
					for (int i2 = 0; i2 < maskList.size(); i2++) {
						if (aux1 == maskList.get(i2))
							trovato = true;
					}
					if (!trovato)
						maskList.add(aux1);
				}
			}
		}
		int[] out = ArrayUtils.arrayListToArrayInt(maskList);
		return out;
	}

}
