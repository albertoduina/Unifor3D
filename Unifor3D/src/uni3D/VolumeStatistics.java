package uni3D;

import java.awt.Checkbox;
import java.util.ArrayList;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import utils.AboutBox;
import utils.ArrayUtils;
import utils.MyLog;
import utils.MyStackUtils;

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
//		boolean selective;
//		selective = false;

		new AboutBox().about("VolumeStatistics", MyVersion.CURRENT_VERSION);
		IJ.wait(2000);
		new AboutBox().close();

		ResultsTable rt = ResultsTable.getResultsTable();

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
//		String defaultItem3 = "";
		String title1 = "";
		String title2 = "";

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
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		int index1 = gd.getNextChoiceIndex();
		title1 = titles[index1];
		int index2 = gd.getNextChoiceIndex();
		title2 = titles[index2];
		ImagePlus impImage = WindowManager.getImage(wList[index1]);
		ImagePlus impMask = WindowManager.getImage(wList[index2]);
		int[] val = singleMaskValues(impMask);
		GenericDialog gd2 = new GenericDialog("Mask Values Selection");

		ArrayList<Integer> intlist = new ArrayList<Integer>();
		ArrayList<String> stringlist = new ArrayList<String>();

		String[] labels = { "0 All-Values-Toghether", "10 Left-Thalamus-Proper", "11 Left-Caudate", "12 Left-Putamen",
				"13 Left-Pallidum", "16 Brain-Stem /4th Ventricle", "17 Left-Hippocampus", "18 Left-Amygdala",
				"26 Left-Accumbens-area", "49 Right-Thalamus-Proper", "50 Right-Caudate", "51 Right-Putamen",
				"52 Right-Pallidum", "53 Right-Hippocampus", "54 Right-Amygdala", "58 Right-Accumbens-area" };

		int[] valuelabels = { 0, 10, 11, 12, 13, 16, 17, 18, 26, 49, 50, 51, 52, 53, 54, 58 };

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

		gd2.addCheckboxGroup(hor, vert, labels2, defaultvalues);
		gd2.showDialog();
		if (gd2.wasCanceled())
			return;

		Vector<Checkbox> checkboxes = gd2.getCheckboxes();

		int selection = 0;
		for (int i1 = 0; i1 < vetint.length; i1++) {
			if ((checkboxes.elementAt(i1).getState() == true)) {
				selection = vetint[i1];
				ArrayList<Integer> pixList = new ArrayList<Integer>();
				pixStackVectorize(impImage, impMask, pixList, selection);
				int[] vetOut = ArrayUtils.arrayListToArrayInt(pixList);
				if (vetOut.length < 2) {
					MyLog.waitHere("per la classe " + selection
							+ " ho troppo pochi pixel selezionati per statisticheggiare! ");
					continue;
				}
				rt.incrementCounter();
				rt.addValue("image", impImage.getTitle());
				rt.addValue("mask", impMask.getTitle());
				rt.addValue("type", labels2[i1]);
				rt.addValue("volume", vetOut.length);
				rt.addValue("min", ArrayUtils.vetMin(vetOut));
				rt.addValue("max", ArrayUtils.vetMax(vetOut));
				rt.addValue("mean", ArrayUtils.vetMean(vetOut));
				rt.addValue("sd", ArrayUtils.vetSdKnuth(vetOut));
				rt.addValue("median", ArrayUtils.vetMedian(vetOut));
				rt.addValue("1_quartile", ArrayUtils.vetQuartile(vetOut, 1));
				rt.addValue("3_quartile", ArrayUtils.vetQuartile(vetOut, 3));
				rt.show("Results");
			}
		}
		rt.show("Results");
		IJ.showMessage("FINE LAVORO");
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
