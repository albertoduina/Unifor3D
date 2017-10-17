package uni3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.TextField;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.DirectoryChooser;
import ij.io.Opener;
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

public class VolumeStatistics implements PlugIn {
	static boolean debug = false;
	final static int timeout = 100;
	static boolean demo1 = false;
	final static boolean step = true;

	public void run(String arg) {
		boolean selective;
		selective = false;

		new AboutBox().about("VolumeStatistics", MyVersion.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

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
		String defaultItem1 = "";
		String defaultItem2 = "";
		String defaultItem3 = "";
		String title1 = "";
		String title2 = "";
		do {
			GenericDialog gd = new GenericDialog("Volume Statistics");
			if (title1.equals(""))
				defaultItem1 = titles[0];
			else
				defaultItem1 = title1;
			gd.addChoice("Images Stack:", titles, defaultItem1);
			if (title2.equals(""))
				defaultItem2 = titles[1];
			else
				defaultItem2 = title2;
			gd.addChoice("Mask Stack:", titles, defaultItem2);

			// 10 Left-Thalamus-Proper 40
			// 11 Left-Caudate 30
			// 12 Left-Putamen 40
			// 13 Left-Pallidum 40
			// 16 Brain-Stem /4th Ventricle 40
			// 17 Left-Hippocampus 30
			// 18 Left-Amygdala 50
			// 26 Left-Accumbens-area 50
			// 49 Right-Thalamus-Proper 40
			// 50 Right-Caudate 30
			// 51 Right-Putamen 40
			// 52 Right-Pallidum 40
			// 53 Right-Hippocampus 30
			// 54 Right-Amygdala 50
			// 58 Right-Accumbens-area 50

			// gd.addStringField("Result:", "Result", 10);
			String[] strValues = { "0  allValues", "10 Left-Thalamus-Proper", "11 Left-Caudate", "12 Left-Putamen",
					"13 Left-Pallidum", "16 Brain-Stem /4th Ventricle", "17 Left-Hippocampus", "18 Left-Amygdala",
					"26 Left-Accumbens-area", "49 Right-Thalamus-Proper", "50 Right-Caudate", "51 Right-Putamen",
					"52 Right-Pallidum", "53 Right-Hippocampus", "54 Right-Amygdala", "58 Right-Accumbens-area" };
			defaultItem3 = "allValues";
			gd.addRadioButtonGroup("MaskValueSelection", strValues, 4, 4, defaultItem3);
			// gd.addCheckbox("32-bit (float) result", floatResult);
			// gd.addHelp(IJ.URL+"/docs/menus/process.html#calculator");
			gd.showDialog();
			if (gd.wasCanceled())
				return;
			int index1 = gd.getNextChoiceIndex();
			title1 = titles[index1];
			// operator = gd.getNextChoiceIndex();
			int index2 = gd.getNextChoiceIndex();
			// String resultTitle = gd.getNextString();
			// selective = gd.getNextBoolean();
			// floatResult = gd.getNextBoolean();
			title2 = titles[index2];
			String aux1 = gd.getNextRadioButton();
			if (aux1 == "")
				aux1 = "0";
			int selection = Integer.parseInt(aux1.replaceAll("\\D", ""));
			// MyLog.waitHere("aux1= " + aux1 + " selection= " + selection);
			ImagePlus impImage = WindowManager.getImage(wList[index1]);
			ImagePlus impMask = WindowManager.getImage(wList[index2]);

			ArrayList<Integer> pixList = new ArrayList<Integer>();
			pixStackVectorize(impImage, impMask, pixList, selection);
			int[] vetOut = ArrayUtils.arrayListToArrayInt(pixList);
			// MyLog.logVectorVertical(vetOut, "maskedPixels");
			if (vetOut.length < 10)
				MyLog.waitHere("ci sono solo " + vetOut.length + " pixels selezionati!!");

			// IJ.log("selectedVolume= " + vetOut.length + " voxels");
			// IJ.log("min= " + ArrayUtils.vetMin(vetOut));
			// IJ.log("max= " + ArrayUtils.vetMax(vetOut));
			// IJ.log("mean= " + ArrayUtils.vetMean(vetOut));
			// IJ.log("sd= " + ArrayUtils.vetSdKnuth(vetOut));
			// IJ.log("median= " + ArrayUtils.vetMedian(vetOut));
			// IJ.log("primo quartile= " + ArrayUtils.vetQuartile(vetOut, 1));
			// IJ.log("terzo quartile= " + ArrayUtils.vetQuartile(vetOut, 3));

			ResultsTable rt = ResultsTable.getResultsTable();
			// rt.reset();
			rt.incrementCounter();
			rt.addValue("image", impImage.getTitle());
			rt.addValue("mask", impMask.getTitle());
			rt.addValue("volume", vetOut.length);
			rt.addValue("min", ArrayUtils.vetMin(vetOut));
			rt.addValue("max", ArrayUtils.vetMax(vetOut));
			rt.addValue("mean", ArrayUtils.vetMean(vetOut));
			rt.addValue("sd", ArrayUtils.vetSdKnuth(vetOut));
			rt.addValue("median", ArrayUtils.vetMedian(vetOut));
			rt.addValue("1_quartile", ArrayUtils.vetQuartile(vetOut, 1));
			rt.addValue("3_quartile", ArrayUtils.vetQuartile(vetOut, 3));
			rt.show("Results");
		} while (true);
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
			ArrayList<Integer> pixList11, int value) {

		ImageProcessor ipSingleImage = impSingleImage.getProcessor();
		ImageProcessor maskSingleImage = impSingleMask.getProcessor();
		float[] imagePixels = (float[]) ipSingleImage.getPixels();
		float[] maskPixels = (float[]) maskSingleImage.getPixels();

		for (int y1 = 0; y1 < impSingleImage.getHeight(); y1++) {
			int offset = y1 * impSingleImage.getHeight();
			for (int x1 = 0; x1 < impSingleImage.getWidth(); x1++) {
				if (value == 0 && maskPixels[offset + x1] != 0 || maskPixels[offset + x1] == (float) value) {
					pixList11.add((int) imagePixels[offset + x1]);
				}
			}
		}
	}

	/**
	 * Restituisce, in un ArrayList fornito come parametro, i valori di tutti i
	 * pixel dello stack immagine sorgente, in cui il valore del pixel
	 * corrispondente nello stack immagine mask sia value (0=qualsiasi valore)
	 * 
	 * @param impSingleImage
	 * @param impSingleMask
	 * @param pixList
	 * @param value
	 */

	public static void pixStackVectorize(ImagePlus impStackImage, ImagePlus impStackMask, ArrayList<Integer> pixList,
			int value) {

		for (int z1 = 1; z1 <= impStackImage.getImageStackSize(); z1++) {
			ImagePlus impSingleImage = MyStackUtils.imageFromStack(impStackImage, z1);
			ImagePlus impSingleMask = MyStackUtils.imageFromStack(impStackMask, z1);
			ImageProcessor ipSingleImage = impSingleImage.getProcessor();
			ImageProcessor maskSingleImage = impSingleMask.getProcessor();
			short[] imagePixels = (short[]) ipSingleImage.getPixels();
			float[] maskPixels = (float[]) maskSingleImage.getPixels();
			for (int y1 = 0; y1 < impSingleImage.getHeight(); y1++) {
				int offset = y1 * impSingleImage.getWidth();
				for (int x1 = 0; x1 < impSingleImage.getWidth(); x1++) {
					int aux1 = (int) maskPixels[offset + x1];
					if ((value == 0 && aux1 != 0)) {
						pixList.add((int) imagePixels[offset + x1]);
					}
					if ((value > 0 && aux1 == value)) {
						pixList.add((int) imagePixels[offset + x1]);
					}
				}
			}
		}
	}

	public static void pixStackVectorizeString(ImagePlus impStackImage, ImagePlus impStackMask,
			ArrayList<String> pixList, int value) {

		for (int z1 = 1; z1 <= impStackImage.getImageStackSize(); z1++) {
			ImagePlus impSingleImage = MyStackUtils.imageFromStack(impStackImage, z1);
			ImagePlus impSingleMask = MyStackUtils.imageFromStack(impStackMask, z1);
			ImageProcessor ipSingleImage = impSingleImage.getProcessor();
			ImageProcessor maskSingleImage = impSingleMask.getProcessor();
			short[] imagePixels = (short[]) ipSingleImage.getPixels();
			float[] maskPixels = (float[]) maskSingleImage.getPixels();

			for (int y1 = 0; y1 < impSingleImage.getHeight(); y1++) {
				int offset = y1 * impSingleImage.getWidth();
				for (int x1 = 0; x1 < impSingleImage.getWidth(); x1++) {
					int aux1 = (int) maskPixels[offset + x1];
					if (value == 0 && aux1 > 1 || aux1 == value) {
						pixList.add((String) "" + (int) imagePixels[offset + x1] + " " + x1 + " " + y1 + " " + z1);
					}
				}
			}
		}
	}

}
